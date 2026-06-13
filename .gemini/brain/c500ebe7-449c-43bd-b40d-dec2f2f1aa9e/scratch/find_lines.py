with open(r'c:\Users\Brent\Antigravity\GENPOX\src\components\PoxConsole.tsx', 'r', encoding='utf-8') as f:
    for i, line in enumerate(f, 1):
        if 'bioLabSubTab' in line or 'bioLabSubTab ===' in line or 'activeTab ===' in line or ('pox' in line.lower() and 'reactor' in line.lower()):
            ascii_line = line.strip().encode('ascii', 'ignore').decode('ascii')
            print(f"{i}: {ascii_line}")
