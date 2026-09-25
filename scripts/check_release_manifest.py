"""Verify the shipped APK, not just the source manifest."""
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
a='{http://schemas.android.com/apk/res/android}'
app=root.find('application')
assert app is not None
assert app.get(a+'debuggable','false')=='false', 'Release must not be debuggable'
assert not list(root.iter('service')), 'No background/accessibility services allowed'
assert not [n for n in root if n.tag.startswith('uses-permission')], 'No requested permissions allowed'
assert root.get('package')=='com.quiet.launcher'
assert root.find('uses-sdk').get(a+'minSdkVersion')=='30'
assert root.find('uses-sdk').get(a+'targetSdkVersion')=='36'
assert app.get(a+'allowBackup')=='false'
assert any(n.get(a+'name')=='android.intent.category.HOME' for n in root.iter('category'))
print('PASS: signed release manifest has no services or requested permissions; HOME registration present.')
