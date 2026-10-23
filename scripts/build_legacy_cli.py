#!/usr/bin/env python3
"""Build a Desktop-1.18.x-only test bundle from the Morphe-2 patch source.

Requires the official Morphe Desktop 1.18.x JAR and local kotlinc (1.9.x).
Not a Morphe Manager bundle: the test archive deliberately lacks classes.dex.
"""
from pathlib import Path
import argparse
import subprocess
import tempfile
import zipfile

SOURCE = Path(__file__).resolve().parents[1] / 'patches/src/main/kotlin/dev/goodlight999/taobaolingo/TaobaoLanguagePatches.kt'

def adapt(source):
    if source.count('isStatic = true') != 5 or source.count('isStatic = false') != 2:
        raise ValueError('Expected seven fingerprints; review source before regenerating')
    source = source.replace('import app.morphe.patcher.Fingerprint',
                            'import com.android.tools.smali.dexlib2.AccessFlags\nimport app.morphe.patcher.Fingerprint', 1)
    source = source.replace('isStatic = true',
                            'accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)', 1)
    source = source.replace('isStatic = false',
                            'accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)', 2)
    source = source.replace('isStatic = true',
                            'accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)', 1)
    source = source.replace('isStatic = true',
                            'accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)', 1)
    source = source.replace('isStatic = true',
                            'accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL)', 1)
    source = source.replace('isStatic = true',
                            'accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)', 1)
    return source

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--desktop-jar', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--kotlinc', default='kotlinc')
    args = parser.parse_args()
    if not args.desktop_jar.is_file():
        parser.error(f'Desktop JAR does not exist: {args.desktop_jar}')
    with tempfile.TemporaryDirectory(prefix='taobaolingo-legacy-') as tmpdir:
        tmpdir = Path(tmpdir)
        src = tmpdir / 'TaobaoLanguagePatches.kt'
        src.write_text(adapt(SOURCE.read_text(encoding='utf-8')), encoding='utf-8')
        classes = tmpdir / 'classes'
        subprocess.run([args.kotlinc, '-Xskip-metadata-version-check', '-Xcontext-receivers',
                        '-cp', str(args.desktop_jar.resolve()), str(src), '-d', str(classes)], check=True)
        manifest = ('Manifest-Version: 1.0\nName: TaobaoLingo\n'
                    'Description: Taobao English localization for legacy Desktop testing\n'
                    'Version: 0.1.0-legacy\nAuthor: GoodLight999\n'
                    'Source: https://github.com/GoodLight999/TaobaoLingo\n'
                    'License: GPL-3.0-only\nPatcher-Version: 1.15.1\n\n')
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(args.out, 'w', zipfile.ZIP_DEFLATED) as bundle:
            bundle.writestr('META-INF/MANIFEST.MF', manifest)
            for path in classes.rglob('*'):
                if path.is_file():
                    bundle.write(path, path.relative_to(classes).as_posix())
    print('Legacy Desktop CLI test bundle:', args.out)

if __name__ == '__main__':
    main()
