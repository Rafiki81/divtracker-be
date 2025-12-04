#!/usr/bin/env python3
"""
Generate all DivTracker architecture diagrams
"""
import subprocess
import sys
from pathlib import Path

def main():
    diagrams_dir = Path(__file__).parent
    
    diagram_files = [
        "aws_architecture.py",
        "backend_components.py", 
        "data_flow.py",
        "fcm_flow.py",
        "entity_relationship.py",
    ]
    
    print("🎨 Generating DivTracker Architecture Diagrams\n")
    print("=" * 50)
    
    success_count = 0
    
    for diagram_file in diagram_files:
        file_path = diagrams_dir / diagram_file
        if not file_path.exists():
            print(f"❌ {diagram_file} - File not found")
            continue
            
        print(f"📊 Generating {diagram_file}...", end=" ")
        
        try:
            result = subprocess.run(
                [sys.executable, str(file_path)],
                cwd=str(diagrams_dir),
                capture_output=True,
                text=True
            )
            
            if result.returncode == 0:
                # Get output filename (same as script but .png)
                output_name = diagram_file.replace(".py", ".png")
                print(f"✅ → {output_name}")
                success_count += 1
            else:
                print(f"❌ Error: {result.stderr}")
                
        except Exception as e:
            print(f"❌ Exception: {e}")
    
    print("=" * 50)
    print(f"\n✨ Generated {success_count}/{len(diagram_files)} diagrams")
    
    if success_count > 0:
        print(f"\n📁 Output files are in: {diagrams_dir}")

if __name__ == "__main__":
    main()
