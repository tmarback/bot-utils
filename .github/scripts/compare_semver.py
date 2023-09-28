import re
import sys
from typing import Tuple

SEMVER_PATTERN = re.compile( r'(\d+)\.(\d+)\.(\d+)' )

def parse_semver( ver: str ) -> Tuple[int, int, int]:

    m = SEMVER_PATTERN.match( ver )
    if not m:
        raise Exception( f"Not a compatible version: {ver}" )
    
    return int( m.group( 1 ) ), int( m.group( 2 ) ), int( m.group( 3 ) )

def compare_semver( ver1: str, ver2: str ) -> int:

    v1_major, v1_minor, v1_patch = parse_semver( ver1 )
    v2_major, v2_minor, v2_patch = parse_semver( ver2 )

    major_diff = v1_major - v2_major
    minor_diff = v1_minor - v2_minor
    patch_diff = v1_patch - v2_patch

    if major_diff != 0:
        return major_diff
    elif minor_diff != 0:
        return minor_diff
    else:
        return patch_diff

if __name__ == '__main__':
    if len( sys.argv ) < 3:
        raise Exception( f"Usage: {sys.argv[0]} <ver1> <ver2>" )
    print( f'{compare_semver( sys.argv[1], sys.argv[2] )}' )