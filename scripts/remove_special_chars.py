# Script to remove special characters from Authentication2.java

# Define the special characters to remove
special_chars = ['❌', '⚠️', '✅', 'ℹ️', '📊', '🎉', '📄']

# Read the original file
with open('src/main/java/Authentication2.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove special characters
for char in special_chars:
    content = content.replace(char, '')

# Write the cleaned content to a new file
with open('src/main/java/Authentication2_clean.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("Special characters removed successfully!")