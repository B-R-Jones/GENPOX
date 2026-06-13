import os

root_dir = r"c:\Users\Brent\Antigravity\GENPOX"
exclude_files = ["search_results.txt", "search_results_utf8.txt", "package-lock.json"]
exclude_dirs = [".git", "node_modules", "pox-android"] # pox-android is already verified clean

replacements = [
    ("codons", "genes"),
    ("Codons", "Genes"),
    ("CODONS", "GENES"),
    ("codon", "gene"),
    ("Codon", "Gene"),
    ("CODON", "GENE"),
]

for root, dirs, files in os.walk(root_dir):
    # Prune excluded directories in-place
    dirs[:] = [d for d in dirs if d not in exclude_dirs]
    
    for file in files:
        if file in exclude_files:
            continue
        
        ext = os.path.splitext(file)[1].lower()
        if ext in (".ts", ".tsx", ".txt", ".html", ".json"):
            filepath = os.path.join(root, file)
            print(f"Processing {filepath}...")
            try:
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original = content
                for target, replacement in replacements:
                    content = content.replace(target, replacement)
                
                if content != original:
                    with open(filepath, 'w', encoding='utf-8', newline='') as f:
                        f.write(content)
                    print(f"Successfully updated {filepath}")
            except Exception as e:
                print(f"Error reading/writing {filepath}: {e}")
