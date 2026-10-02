#!/usr/bin/env python3
"""Build the Android-executed `yt-dlp` zipapp from the vendored source tree.

Replicates the relevant part of ``yt-dlp/Makefile`` in pure Python so it
works on Windows / Linux / macOS without ``zip``/``touch``/``cat``:

1. (optionally) regenerate ``yt_dlp/extractor/lazy_extractors.py``
   via ``devscripts/make_lazy_extractors.py`` (same as ``make lazy-extractors``);
2. stage ``yt_dlp/**/*.py`` into a temp ``zip/`` layout:
   - every package dir (a dir containing ``__init__.py``) contributes ``*.py``;
   - ``yt_dlp/__main__.py`` is REMOVED from the package and placed at the
     zip root as ``__main__.py`` (this is what makes ``python yt-dlp`` work);
3. write a deterministic zip (fixed timestamp ``200001010101`` like the
   Makefile) and prepend ``#!/usr/bin/env python3\\n`` to produce the final
   ``yt-dlp`` single-file executable used by youtubedl-android
   (``python libpython.so <.../yt-dlp> <args>``);
4. write ``<out>.version`` (from ``yt_dlp/version.py``) and
   ``<out>.sha256`` for the runtime installer to verify.

Usage (run from the ``yt-dlp`` checkout root, or pass --source):

    python build_yt_dlp.py --source F:/ZionWorkspace/Seal/yt-dlp \\
        --out app/build/generated/ytdlp/yt-dlp [--no-lazy] [--lazy-out <file>]

Exit codes: 0 ok, 2 bad args, 3 build failure.
"""

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

FIXED_DATE = (1980, 1, 1, 1, 1, 1)  # ~ Makefile `touch -t 200001010101`
SHEBANG = b'#!/usr/bin/env python3\n'
VERSION_RE = re.compile(r"""^_pkg_version\s*=\s*['"]([^'"]+)['"]""", re.M)


def read_version(source: Path) -> str:
    text = (source / 'yt_dlp' / 'version.py').read_text(encoding='utf-8')
    m = VERSION_RE.search(text)
    if not m:
        raise RuntimeError('cannot parse _pkg_version from yt_dlp/version.py')
    return m.group(1)


def package_dirs(source: Path) -> list[Path]:
    """Dirs that contain __init__.py, relative to source root (mirrors Makefile)."""
    result = []
    for init in sorted((source / 'yt_dlp').rglob('__init__.py')):
        result.append(init.parent.relative_to(source))
    return result


def ensure_lazy_extractors(source: Path, regen: bool) -> Path | None:
    target = source / 'yt_dlp' / 'extractor' / 'lazy_extractors.py'
    if not regen:
        return target if target.exists() else None
    script = source / 'devscripts' / 'make_lazy_extractors.py'
    # Generate to a temp file first so a failed run never corrupts the tree.
    with tempfile.NamedTemporaryFile(
        suffix='.py', prefix='lazy_extractors_', delete=False
    ) as tmp:
        tmp_path = Path(tmp.name)
    try:
        subprocess.run(
            [sys.executable, str(script), str(tmp_path)],
            cwd=str(source),
            check=True,
            capture_output=True,
            text=True,
        )
    except subprocess.CalledProcessError as e:
        tmp_path.unlink(missing_ok=True)
        raise RuntimeError(f'make_lazy_extractors failed: {e.stderr[-2000:]}') from e
    # NOTE: tmp file may live on another drive (Windows TEMP vs repo drive),
    # so copy bytes instead of os-rename across volumes.
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(tmp_path.read_bytes())
    tmp_path.unlink(missing_ok=True)
    return target


def collect_files(source: Path) -> list[Path]:
    files: list[Path] = []
    for pkg in package_dirs(source):
        for py in sorted((source / pkg).glob('*.py')):
            # Handled specially: root __main__ for the zipapp entry point.
            if pkg.as_posix() == 'yt_dlp' and py.name == '__main__.py':
                continue
            files.append(py.relative_to(source))
    return files


def build_zipapp(source: Path, out: Path) -> tuple[str, str]:
    version = read_version(source)
    files = collect_files(source)
    if not files:
        raise RuntimeError('no yt_dlp python files found')
    main_py = source / 'yt_dlp' / '__main__.py'
    if not main_py.exists():
        raise RuntimeError('yt_dlp/__main__.py missing')

    out.parent.mkdir(parents=True, exist_ok=True)
    tmp_zip = out.with_suffix('.zip.tmp')
    with zipfile.ZipFile(tmp_zip, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        # 1. package files, deterministic order + timestamp.
        for rel in files:
            zi = zipfile.ZipInfo(rel.as_posix(), date_time=FIXED_DATE)
            zi.compress_type = zipfile.ZIP_DEFLATED
            zi.external_attr = 0o644 << 16
            z.writestr(zi, (source / rel).read_bytes())
        # 2. zipapp entry point at the archive root.
        zi = zipfile.ZipInfo('__main__.py', date_time=FIXED_DATE)
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o644 << 16
        z.writestr(zi, main_py.read_bytes())

    # 3. prepend shebang -> final `yt-dlp` executable.
    digest = hashlib.sha256()
    with open(tmp_zip, 'rb') as fz, open(out, 'wb') as fo:
        fo.write(SHEBANG)
        digest.update(SHEBANG)
        while True:
            chunk = fz.read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)
            fo.write(chunk)
    tmp_zip.unlink(missing_ok=True)

    sha = digest.hexdigest()
    out.with_suffix(out.suffix + '.version' if out.suffix else '.version')
    (out.parent / (out.name + '.version')).write_text(version + '\n', encoding='utf-8')
    (out.parent / (out.name + '.sha256')).write_text(sha + '\n', encoding='utf-8')
    return version, sha


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description='Build yt-dlp zipapp for Seal/Android.')
    ap.add_argument('--source', required=True, help='yt-dlp checkout root')
    ap.add_argument('--out', required=True, help='output `yt-dlp` file')
    ap.add_argument(
        '--no-lazy',
        action='store_true',
        help='skip regenerating lazy_extractors.py (use tree as-is)',
    )
    args = ap.parse_args(argv)
    source = Path(args.source)
    if not (source / 'yt_dlp' / '__init__.py').exists():
        print(f'error: not a yt-dlp checkout: {source}', file=sys.stderr)
        return 2
    try:
        lazy = ensure_lazy_extractors(source, regen=not args.no_lazy)
        if lazy is None:
            print(
                'warning: lazy_extractors.py missing and --no-lazy given; '
                'startup will be slower but the build still works.',
                file=sys.stderr,
            )
        version, sha = build_zipapp(source, Path(args.out))
    except Exception as e:  # noqa: BLE001 - CLI surface
        print(f'error: {e}', file=sys.stderr)
        return 3
    print(f'yt-dlp {version} -> {args.out} (sha256 {sha[:16]}...)')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
