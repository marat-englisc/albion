"""Small ADB helper for inspecting the disposable design-review emulator."""
import re
import subprocess
import time
from pathlib import Path
from xml.etree import ElementTree as ET

ADB = r'C:\Users\Marat\AppData\Local\Android\Sdk\platform-tools\adb.exe'
OUT = Path(__file__).resolve().parent

def adb(*args):
    return subprocess.run([ADB, '-s', 'emulator-5556', *map(str, args)],
                          check=True, capture_output=True).stdout

def root():
    adb('shell', 'uiautomator', 'dump', '/sdcard/albion-design-ui.xml')
    return ET.fromstring(adb('exec-out', 'cat', '/sdcard/albion-design-ui.xml'))

def bounds(node):
    return list(map(int, re.findall(r'\d+', node.get('bounds', ''))))

def click(text=None, desc=None):
    tree = root()
    for node in tree.iter('node'):
        if (text is None or node.get('text') == text) and (desc is None or node.get('content-desc') == desc):
            x1, y1, x2, y2 = bounds(node)
            if x2 > x1 and y2 > y1:
                adb('shell', 'input', 'tap', (x1+x2)//2, (y1+y2)//2)
                time.sleep(0.45)
                return
    raise ValueError(f'Element not found: text={text!r}, desc={desc!r}. Visible: {texts(tree)}')

def texts(tree=None):
    return [n.get('text') or n.get('content-desc') for n in (tree if tree is not None else root()).iter('node')
            if n.get('text') or n.get('content-desc')]

def tab(label):
    matches = [n for n in root().iter('node') if n.get('text') == label or n.get('content-desc') == label]
    x1, y1, x2, y2 = bounds(matches[-1])
    adb('shell', 'input', 'tap', (x1+x2)//2, (y1+y2)//2)
    time.sleep(0.4)

def capture(name):
    tree = root()
    time.sleep(0.35)
    (OUT / f'{name}.png').write_bytes(adb('exec-out', 'screencap', '-p'))
    (OUT / f'{name}.xml').write_bytes(ET.tostring(tree, encoding='utf-8'))
    print(name, texts(tree))

def scroll(up=True):
    adb('shell', 'input', 'swipe', 540, 1550 if up else 650, 540, 650 if up else 1550, 450)
    time.sleep(0.3)

if __name__ == '__main__':
    capture('initial')
