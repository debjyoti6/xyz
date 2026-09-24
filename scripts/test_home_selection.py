"""Exercise the real system chooser. Never assign HOME through shell commands."""
import subprocess, time, pathlib, xml.etree.ElementTree as ET, re, json
OUT=pathlib.Path('home-test-results'); OUT.mkdir(exist_ok=True)
PKG='com.quiet.launcher'
def adb(*args):
    return subprocess.check_output(['adb',*args],stderr=subprocess.STDOUT,text=True,timeout=30).strip()
def dump(name):
    adb('shell','uiautomator','dump','/sdcard/quiet-window.xml')
    xml=adb('shell','cat','/sdcard/quiet-window.xml')
    (OUT/(name+'.xml')).write_text(xml)
    with (OUT/(name+'.png')).open('wb') as f:
        subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True,timeout=20)
    return ET.fromstring(xml)
def tap(node):
    a=list(map(int,re.findall(r'\d+',node.attrib['bounds'])))
    adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2))
def default():
    return adb('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
def wait_default():
    for _ in range(20):
        if PKG+'/' in default(): return True
        time.sleep(.5)
    return False
def external_quiet(root):
    return next((n for n in root.iter('node') if n.get('text')=='Quiet Launcher' and n.get('package')!=PKG),None)
try:
    adb('shell','input','keyevent','KEYCODE_WAKEUP'); adb('shell','wm','dismiss-keyguard')
    adb('shell','am','start','-W','-n',PKG+'/.MainActivity')
    time.sleep(2)
    root=dump('01-before-request')
    button=next((n for n in root.iter('node') if n.get('text') in ('Choose default home app','Set Quiet as your home screen')),None)
    if button is None:
        # Welcome can scroll on small screens.
        adb('shell','input','swipe','160','540','160','170','300'); time.sleep(.5)
        root=dump('02-scrolled')
        button=next((n for n in root.iter('node') if n.get('text') in ('Choose default home app','Set Quiet as your home screen')),None)
    assert button is not None,'Home setup button was not visible'
    tap(button); time.sleep(2)
    root=dump('03-setup')
    request=next((n for n in root.iter('node') if n.get('text')=='Choose Quiet as default Home'),None)
    assert request is not None,'Dedicated Home setup did not open'
    tap(request); time.sleep(2)
    root=dump('03-system-chooser')
    assert external_quiet(root) is not None,'Quiet missing from chooser before cancellation'
    adb('shell','input','keyevent','KEYCODE_BACK'); time.sleep(1)
    root=dump('03-cancelled')
    assert PKG+'/' not in default(),'Cancelling must not change default Home'
    request=next((n for n in root.iter('node') if n.get('text')=='Choose Quiet as default Home'),None)
    assert request is not None,'Cannot retry after cancelling chooser'
    tap(request); time.sleep(2)
    root=dump('03-retry-chooser')
    choice=external_quiet(root)
    assert choice is not None,'Android did not show Quiet as a selectable HOME app'
    tap(choice); time.sleep(.7)
    root=dump('04-quiet-selected')
    confirm=next((n for n in root.iter('node') if n.get('package')!=PKG and n.get('enabled')=='true' and
        (n.get('text','').lower() in ('set as default','always','ok') or n.get('resource-id')=='android:id/button1')),None)
    if confirm is not None: tap(confirm)
    assert wait_default(),'Quiet was selected but Android did not make it the default HOME resolver'
    adb('shell','input','keyevent','KEYCODE_HOME'); time.sleep(2)
    root=dump('05-home-pressed')
    assert any(n.get('package')==PKG for n in root.iter('node')),'Home did not display Quiet'
    # Verify the preference survives leaving the app and starting Home again.
    adb('shell','am','start','-W','-a','android.settings.SETTINGS'); time.sleep(.5)
    adb('shell','input','keyevent','KEYCODE_HOME'); time.sleep(1)
    root=dump('06-home-from-settings')
    assert PKG+'/' in default()
    assert any(n.get('package')==PKG for n in root.iter('node'))
    result={'status':'passed','sdk':adb('shell','getprop','ro.build.version.sdk'),'home':default()}
    (OUT/'result.json').write_text(json.dumps(result,indent=2));print(result)
except Exception:
    try: dump('failure')
    except Exception: pass
    (OUT/'logcat.txt').write_text(adb('logcat','-d','-t','2000'))
    raise
