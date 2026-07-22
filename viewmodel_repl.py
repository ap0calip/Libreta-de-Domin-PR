import re

app_file = "app/src/main/java/com/example/ui/DominoViewModel.kt"
with open(app_file, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace('"Nosotr@s"', 'com.example.ui.MyApp.context.getString(com.example.R.string.nosotr_s)')
content = content.replace('"Ell@s"', 'com.example.ui.MyApp.context.getString(com.example.R.string.ell_s)')
# wait, there's no MyApp.context. I shouldn't break the build if there is no context in ViewModel.

print(content[:200])
