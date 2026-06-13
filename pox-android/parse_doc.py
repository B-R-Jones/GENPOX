import re

html_path = r"C:\Users\Brent\.gemini\antigravity-ide\brain\c500ebe7-449c-43bd-b40d-dec2f2f1aa9e\.system_generated\steps\936\content.md"

with open(html_path, "r", encoding="utf-8") as f:
    content = f.read()

# Let's clean the markdown wrapper if any
content = content.replace("Title: Live Content\n\nDescription: Fetched live\n\nSource: https://docs.google.com/document/d/1eYaRzBxl0eNcFbF-Jg626-E3tl4ZKttJv126Njt2drI/export?format=html\n\n---\n\n", "")

print("HTML length:", len(content))

start_str = "If Calendar Day of Month is within"
end_str = "it has a"
pos_start = content.find(start_str)
pos_end = content.find(end_str)
if pos_start != -1 and pos_end != -1:
    print("\nHTML Segment:")
    print(content[pos_start:pos_end + len(end_str)])
else:
    print("Not found", pos_start, pos_end)


