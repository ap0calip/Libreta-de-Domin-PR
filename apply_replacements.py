import re

app_file = "app/src/main/java/com/example/ui/DominoApp.kt"
with open(app_file, "r", encoding="utf-8") as f:
    content = f.read()

# Load strings from new_strings.xml
strings_map = {}
with open("new_strings.xml", "r", encoding="utf-8") as f:
    for line in f:
        match = re.search(r'<string name="([^"]+)">([^<]+)</string>', line)
        if match:
            k, v = match.group(1), match.group(2)
            # unescape
            v = v.replace('&amp;', '&').replace('&lt;', '<').replace('&gt;', '>').replace("\\'", "'").replace('\\"', '"')
            if k == "app_name": continue
            if k == "string": continue # skip " - "
            strings_map[v] = k

def replace_simple_string(match):
    prefix = match.group(1)
    string_content = match.group(2)
    suffix = match.group(3)
    
    if string_content in strings_map:
        key = strings_map[string_content]
        return f'{prefix}stringResource(id = R.string.{key}){suffix}'
    return match.group(0)

# Replace in Text("...")
content = re.sub(r'(Text\s*\(\s*)("([^"\\]*(?:\\.[^"\\]*)*)")(\s*[\),])', replace_simple_string, content)
# Replace in Text(text = "...")
content = re.sub(r'(Text\s*\(\s*text\s*=\s*)("([^"\\]*(?:\\.[^"\\]*)*)")(\s*[\),])', replace_simple_string, content)
# Replace in contentDescription = "..."
content = re.sub(r'(contentDescription\s*=\s*)("([^"\\]*(?:\\.[^"\\]*)*)")(\s*[\),])', replace_simple_string, content)

with open("app/src/main/java/com/example/ui/DominoApp.kt", "w", encoding="utf-8") as f:
    f.write(content)
