#!/usr/bin/env python
import sys
from os.path import dirname, abspath, join, exists
import os
from xml.etree.ElementTree import ElementTree

CURRENT_DIR = dirname(__file__)
DEFAULT_M2_REPO = abspath(join(os.environ['HOME'], '.m2', 'repository'))
M2_REPO = os.environ.get('M2_REPO', DEFAULT_M2_REPO)


def get_real_path(cp_entry):
    """Returns the actual path of the classpath entry"""
    cp_entry_kind = cp_entry['kind']
    cp_entry_path = cp_entry['path']

    if cp_entry_kind == 'var':
        cp_entry_path = cp_entry_path.replace('M2_REPO', M2_REPO)

    elif cp_entry_kind == 'output':
        cp_entry_path = abspath(
            join(CURRENT_DIR, *cp_entry_path.split(os.sep)))

    elif cp_entry_kind == 'src' and 'resources' in cp_entry_path:
        cp_entry_path = abspath(
            join(CURRENT_DIR, *cp_entry_path.split(os.sep)))
    else:
        cp_entry_path = ''

    return cp_entry_path


def get_useful_entries(cp_entries):
    """Get rid of eclipse specific stuff e.g. container"""
    useful_entries = []
    for cp_entry in cp_entries:
        if cp_entry['kind'] in ('src', 'var', 'output'):
            useful_entries.append(get_real_path(cp_entry))
    return filter(len, useful_entries)

if __name__ == '__main__':
    classpath_file = abspath(join(dirname(__file__), '.classpath'))
    if exists(classpath_file):
        xml_document = ElementTree().parse(classpath_file)
        cp_entries = xml_document.findall('classpathentry')
        attrib_dicts = map(lambda x: x.attrib, cp_entries)
        print ':'.join(sorted(get_useful_entries(attrib_dicts)))
    else:
        print 'There\'s no .classpath file available in the project!'
        sys.exit(0)
