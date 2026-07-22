import re

app_file = "app/src/main/java/com/example/ui/DominoApp.kt"
with open(app_file, "r", encoding="utf-8") as f:
    content = f.read()

# All double-quoted strings (no newlines inside)
all_strings = set(re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', content))

valid_strings = {}
counter = 1

def generate_key(text):
    global counter
    base = re.sub(r'\$\{.*?\}|\$[a-zA-Z0-9_]+', '', text)
    base = re.sub(r'[^a-zA-Z0-9]', '_', base).strip('_').lower()
    base = re.sub(r'_+', '_', base)
    base = base[:20]
    if not base:
        base = "string"
    
    key = base
    while key in valid_strings:
        key = f"{base}_{counter}"
        counter += 1
    return key

# Add known strings from our previous run
# We want to exclude empty strings, purely numeric ones, or ones that look like code
for s in all_strings:
    s_strip = s.strip()
    if len(s_strip) > 0 and not s_strip.isnumeric() and s_strip not in ["PAREJAS", "COMPLETED", "CANCELED", "INDIVIDUAL", "DOMINACION", "CAPICU", "CHUCHAZO", "Dashboard", "ScreenTransition", " y "]:
        if "{" not in s and "$" not in s and "%" not in s and "/" not in s and "://" not in s:
            if s.islower() and "_" in s: continue # likely resource id or something else
            key = generate_key(s)
            valid_strings[key] = s

print("<resources>")
print('    <string name="app_name">Dominó</string>')
for k, v in valid_strings.items():
    # xml escape
    v_esc = v.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('\'', "\\'").replace('"', '\\"')
    print(f'    <string name="{k}">{v_esc}</string>')
print("</resources>")
