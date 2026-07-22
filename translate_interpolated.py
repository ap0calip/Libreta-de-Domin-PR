import re

app_file = "app/src/main/java/com/example/ui/DominoApp.kt"
with open(app_file, "r", encoding="utf-8") as f:
    content = f.read()

# Let's find all Text(text = "...") that have interpolation.
interpolated_texts = re.findall(r'(Text\s*\(\s*(?:text\s*=\s*)?)("([^"\\]*(?:\\.[^"\\]*)*)")', content)

for match in interpolated_texts:
    if "$" in match[2]:
        print(match[2])
