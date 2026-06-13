import os

root_dir = r"c:\Users\Brent\Antigravity\GENPOX"
exclude_files = ["search_results.txt", "search_results_utf8.txt", "package-lock.json"]
exclude_dirs = [".git", "node_modules", "app\\build", ".gradle", ".kotlin", "pox-android\\.gradle", "pox-android\\app\\build"]

# We will also target specific markdown files in the artifacts directory
artifact_doc = r"C:\Users\Brent\.gemini\antigravity-ide\brain\c500ebe7-449c-43bd-b40d-dec2f2f1aa9e\scientific_formulas.md"

replacements = [
    # Plural and combination replacements (longest to shortest)
    ("anomalous gene cores", "anomalous genes"),
    ("Anomalous Gene Cores", "Anomalous Genes"),
    ("ANOMALOUS GENE CORES", "ANOMALOUS GENES"),
    ("gene cores", "genes"),
    ("Gene Cores", "Genes"),
    ("GENE CORES", "GENES"),
    ("gene core", "gene"),
    ("Gene Core", "Gene"),
    ("GENE CORE", "GENE"),
    ("new cores", "new genes"),
    ("New Cores", "New Genes"),
    ("NEW CORES", "NEW GENES"),
    ("Quantum Extraction Core", "Quantum Extraction Unit"),
    ("Quantum Extraction Cores", "Quantum Extraction Units"),
    ("QUANTUM EXTRACTION CORE", "QUANTUM EXTRACTION UNIT"),
    ("Peer Node Shield Core", "Peer Node Shield Unit"),
    ("Chitin-Core", "Chitin-Shell"),
    ("Chitin-core", "Chitin-shell"),
    ("AI Core", "AI Engine"),
    ("AI CORE", "AI ENGINE"),
    ("Core Anomaly", "Anomaly"),
    ("CORE ANOMALY", "ANOMALY"),
    ("compilation core", "compilation system"),
    ("offline compiler core", "offline compiler engine"),
    ("Core power", "Reactor power"),
    ("core power", "reactor power"),
    ("Core Power", "Reactor Power"),
    ("CORE POWER", "REACTOR POWER"),
    ("Spliced Core", "Spliced Gene"),
    ("SPLICED CORE", "SPLICED GENE"),
    ("CORES RUNNING", "SYSTEMS RUNNING"),
    ("defender core", "defender"),
    ("your core", "your unit"),
    ("attacker core", "attacker"),
    ("Core components", "System components"),
    ("Core Components", "System Components"),
    ("CORE COMPONENTS", "SYSTEM COMPONENTS"),
    ("System Core Down", "System Offline"),
    ("SYSTEM CORE DOWN", "SYSTEM OFFLINE"),
    ("core genetic processes", "genetic processes"),
    ("Core Genetic Processes", "Genetic Processes"),
    ("CENTER CORE", "CENTER NODE"),
    ("FROM CORE", "FROM NODE"),
    ("From Core", "From Node"),
    ("From core", "From node"),
    ("from core", "from node"),
    ("CORE SECTOR", "SECTOR"),
    ("DECRYPTION CORE", "DECRYPTION ENGINE"),
    ("Response Core", "Response"),
    ("core base pair", "base-pair"),
    ("core biological combination", "biological combination"),
    ("representing core", "representing node"),
    ("G.E.N.P.O.X. CORE", "G.E.N.P.O.X. MAIN"),
    ("AUTOTRONIC MORPHOGENESIS CORE", "AUTOTRONIC MORPHOGENESIS ENGINE"),
    ("core sequence", "gene sequence"),
    ("Core sequence", "Gene sequence"),
    ("CORE SEQUENCE", "GENE SEQUENCE"),
    ('"Core"', '"Shell"'),
    # General replacements (word bound or specific placeholders)
    ("cores", "systems"),
    ("Cores", "Systems"),
    ("CORES", "SYSTEMS"),
    ("core", "unit"),
    ("Core", "Unit"),
    ("CORE", "UNIT"),
]

def process_file(filepath):
    print(f"Processing {filepath}...")
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        modified = False
        new_lines = []
        for line in lines:
            # Check if this line is an import statement or library usage we should skip
            if ("import " in line or "package " in line) and ("androidx." in line or "android." in line or "datastore" in line or "camera" in line or "compose" in line):
                new_lines.append(line)
                continue
            
            # Apply replacements to the line
            original_line = line
            for target, replacement in replacements:
                line = line.replace(target, replacement)
            
            if line != original_line:
                modified = True
            new_lines.append(line)
            
        if modified:
            with open(filepath, 'w', encoding='utf-8', newline='') as f:
                f.writelines(new_lines)
            print(f"Successfully updated {filepath}")
    except Exception as e:
        print(f"Error processing {filepath}: {e}")

# Walk directories
for root, dirs, files in os.walk(root_dir):
    # Prune excluded directories in place
    dirs[:] = [d for d in dirs if not any(root.endswith(ed) or ed in os.path.join(root, d) for ed in exclude_dirs)]
    
    for file in files:
        if file in exclude_files:
            continue
        
        ext = os.path.splitext(file)[1].lower()
        if ext in (".ts", ".tsx", ".txt", ".html", ".json", ".kt"):
            filepath = os.path.join(root, file)
            # Skip build/cache dirs double safety
            if any(ed in filepath for ed in exclude_dirs):
                continue
            process_file(filepath)

# Process scientific formulas doc
if os.path.exists(artifact_doc):
    process_file(artifact_doc)
