#!/usr/bin/env python3
"""
Spark Bug Doctor - Terminal diagnostic helper for SparkX FairyOS build errors.

Usage:
    gradle clean assembleDebug --stacktrace 2>&1 | tee build.log
    python3 scripts/bug_doctor.py build.log

Or pipe directly:
    gradle clean assembleDebug --stacktrace 2>&1 | python3 scripts/bug_doctor.py
"""

from pathlib import Path
import sys

ERROR_PATTERNS = [
    {
        "match": "'val' cannot be reassigned",
        "title": "Read-only val reassigned",
        "fix": "A function parameter or val is being assigned. Move mutation to parent state or change local remembered state to var.",
    },
    {
        "match": "Unresolved reference",
        "title": "Missing reference",
        "fix": "Check package, import, filename, or missing fallback function.",
    },
    {
        "match": "No parameter with name",
        "title": "Composable signature mismatch",
        "fix": "Update the called function signature to accept the named parameter.",
    },
    {
        "match": "Type mismatch",
        "title": "Type mismatch",
        "fix": "Change the parameter type or convert the value before passing it.",
    },
    {
        "match": "gradlew",
        "title": "Possible Gradle wrapper issue",
        "fix": "Use global gradle temporarily, then regenerate wrapper.",
    },
]

def diagnose(text: str) -> None:
    found = False
    for item in ERROR_PATTERNS:
        if item["match"].lower() in text.lower():
            found = True
            print(f"\nSpark Bug Doctor: {item['title']}")
            print(f"Fix: {item['fix']}")
    if not found:
        print("\nSpark Bug Doctor: Unknown error.")
        print("Paste the first red compiler block into Teach & Grow and add the verified fix after repair.")

def main():
    if len(sys.argv) > 1:
        text = Path(sys.argv[1]).read_text(errors="ignore")
    else:
        text = sys.stdin.read()
    diagnose(text)

if __name__ == "__main__":
    main()