# Script to clean encoding issues in Egalvanic.java
import re

# Read the file
with open('src/main/java/Egalvanic.java', 'r', encoding='utf-8', errors='ignore') as f:
    content = f.read()

# Replace problematic characters with appropriate symbols
# Replace the garbled characters with proper symbols
replacements = [
    ('ΓÜáΓÖ¢', '⚠️'),
    ('Γ£î', '❌'),
    ('ΓÉÆ', '➡'),
    ('Γ¥Ñ', '🗑'),
    ('Γ£║', '✔'),
    ('ΓÇô', '-'),
    ('ΓÇö', ''),
    ('ΓÇï', ''),
    ('ΓÇó', ''),
    ('Γ?î', '❌'),
    ('ΓÜá∩╕?', '⚠️'),
    ('Γä╣∩╕?', 'ℹ️'),
    ('ΓÇö', ''),
]

# Apply replacements
for old, new in replacements:
    content = content.replace(old, new)

# Write the cleaned content back to the file
with open('src/main/java/Egalvanic.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("File cleaned successfully!")