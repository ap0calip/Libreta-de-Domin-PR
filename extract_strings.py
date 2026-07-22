import re
import os

file_path = "app/src/main/java/com/example/ui/DominoApp.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Pattern for Text(text = "...") or Text("...") or contentDescription = "..."
# We will just collect all double-quoted strings for now to see what we have
strings = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', content)

# Filter out empty strings, package names, single characters (unless words), formatting
filtered = set()
for s in strings:
    if len(s.strip()) > 1 and not re.match(r'^[a-zA-Z0-9_\.]+$', s) and "{" not in s and "$" not in s and "%" not in s:
        if s.isupper():
            if len(s) > 10: filtered.add(s)
        else:
            filtered.add(s)

for s in sorted(filtered):
    print(s)

