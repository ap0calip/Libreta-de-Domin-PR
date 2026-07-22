import re
import os

app_file = "app/src/main/java/com/example/ui/DominoApp.kt"
with open(app_file, "r", encoding="utf-8") as f:
    content = f.read()

strings_dict = {}
counter = 1

def generate_key(text):
    global counter
    # Create a basic key from text
    # Remove variables ${...} and $var
    base = re.sub(r'\$\{.*?\}|\$[a-zA-Z0-9_]+', '', text)
    base = re.sub(r'[^a-zA-Z0-9]', '_', base).strip('_').lower()
    base = re.sub(r'_+', '_', base)
    base = base[:20]
    if not base:
        base = "string"
    
    key = base
    while key in strings_dict.values():
        key = f"{base}_{counter}"
        counter += 1
    return key

# We want to replace instances of "string" with stringResource(id = R.string.key)
# We have several cases:
# 1. Text("...")
# 2. Text(text = "...")
# 3. contentDescription = "..."
# 4. label = { Text("...") }

def replace_simple_string(match):
    prefix = match.group(1)
    string_content = match.group(2)
    suffix = match.group(3)
    
    if "$" in string_content or string_content.strip() == "":
        return match.group(0) # Skip interpolated or empty strings for now
        
    if string_content in [v[0] for v in strings_dict.values()]:
        # Already exists
        key = [k for k, v in strings_dict.items() if v[0] == string_content][0]
    else:
        key = generate_key(string_content)
        strings_dict[key] = (string_content, False) # False means no format args
        
    return f'{prefix}stringResource(id = R.string.{key}){suffix}'

# Text("...")
content = re.sub(r'(Text\s*\(\s*)("([^"\\]*(?:\\.[^"\\]*)*)")(\s*[\),])', replace_simple_string, content)
# Text(text = "...")
content = re.sub(r'(Text\s*\(\s*text\s*=\s*)("([^"\\]*(?:\\.[^"\\]*)*)")(\s*[\),])', replace_simple_string, content)
# contentDescription = "..."
content = re.sub(r'(contentDescription\s*=\s*)("([^"\\]*(?:\\.[^"\\]*)*)")(\s*[\),])', replace_simple_string, content)

with open("app_modified.kt", "w", encoding="utf-8") as f:
    f.write(content)

print(f"Extracted {len(strings_dict)} simple strings.")
for k, v in strings_dict.items():
    print(f"{k}: {v[0]}")
