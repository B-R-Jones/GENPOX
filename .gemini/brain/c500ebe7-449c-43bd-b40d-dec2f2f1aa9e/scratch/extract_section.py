with open(r'c:\Users\Brent\Antigravity\GENPOX\src\components\PoxConsole.tsx', 'r', encoding='utf-8') as f:
    lines = f.readlines()

start = 5040
end = 5820
extracted = lines[start - 1 : end]

with open(r'C:\Users\Brent\.gemini\antigravity-ide\brain\c500ebe7-449c-43bd-b40d-dec2f2f1aa9e\scratch\extracted_console.txt', 'w', encoding='utf-8') as out:
    for i, line in enumerate(extracted, start):
        out.write(f"{i}: {line}")
print("Successfully extracted console code section.")
