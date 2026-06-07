#!/bin/bash
# SparkX FairyOS v7 Preflight Safety Check
# Fails build if dangerous patterns are found (privacy violations)

set -e

echo "🔍 Running SparkX FairyOS Preflight Safety Check..."

ERRORS=0

# 1. Check for INTERNET permission in manifest (allowed only because optional AI, but warn)
if grep -q "android.permission.INTERNET" app/src/main/AndroidManifest.xml; then
  echo "⚠️  WARNING: INTERNET permission present (required for optional user-enabled cloud AI only). Ensure no automatic calls."
fi

# 2. Check for AccessibilityService mentions (must be commented/disabled)
if grep -r "AccessibilityService" app/src/main --include="*.kt" --include="*.xml" | grep -v "commented" | grep -v "Future scaffold" | grep -v "disabled by default"; then
  echo "❌ ERROR: Active AccessibilityService code found. This must remain disabled scaffold only."
  ERRORS=$((ERRORS+1))
fi

# 3. Check for screenshot / screen capture APIs
if grep -rE "MediaProjection|MediaRecorder.*screen|ScreenCapture|takeScreenshot|WindowManager.*screenshot" app/src/main --include="*.kt"; then
  echo "❌ ERROR: Screen capture / screenshot code detected. Forbidden."
  ERRORS=$((ERRORS+1))
fi

# 4. Check for network calls outside AI client (basic heuristic)
if grep -rE "okhttp|HttpURLConnection|Retrofit|fetch\(|volley" app/src/main --include="*.kt" | grep -v "SparkAIClient" | grep -v "ai/"; then
  echo "❌ ERROR: Network code outside approved AI client. Forbidden in core."
  ERRORS=$((ERRORS+1))
fi

# 5. Check required files exist
REQUIRED_FILES=(
  "app/src/main/AndroidManifest.xml"
  "app/src/main/java/com/sparkx/fairyos/MainActivity.kt"
  "app/src/main/java/com/sparkx/fairyos/SparkXApplication.kt"
  "app/src/main/java/com/sparkx/fairyos/ui/components/SparkBabyAvatar.kt"
  "app/src/main/java/com/sparkx/fairyos/overlay/SparkOverlayService.kt"
)

for f in "${REQUIRED_FILES[@]}"; do
  if [ ! -f "$f" ]; then
    echo "❌ ERROR: Missing required file: $f"
    ERRORS=$((ERRORS+1))
  fi
done

if [ $ERRORS -gt 0 ]; then
  echo "❌ Preflight FAILED with $ERRORS error(s). Fix before release."
  exit 1
else
  echo "✅ Preflight PASSED. Safe to build."
  exit 0
fi
