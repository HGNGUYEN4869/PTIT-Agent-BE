# Arduino CLI Auto Installer for Windows
# Chạy script này trong PowerShell với quyền Administrator

# 1. Download Arduino CLI
$url = "https://github.com/arduino/arduino-cli/releases/download/v1.1.2/arduino-cli_1.1.2_Windows_64bit.zip"
$output = "$env:TEMP\arduino-cli.zip"
$extractPath = "$env:TEMP\arduino-cli"

Write-Host "📥 Downloading Arduino CLI..." -ForegroundColor Green
Invoke-WebRequest -Uri $url -OutFile $output

# 2. Giải nén
Write-Host "📦 Extracting..." -ForegroundColor Green
Expand-Archive -Path $output -DestinationPath $extractPath -Force

# 3. Tạo thư mục cài đặt
$installPath = "C:\Program Files\ArduinoCLI"
New-Item -ItemType Directory -Force -Path $installPath | Out-Null

# 4. Copy file
Write-Host "📁 Installing to $installPath..." -ForegroundColor Green
Copy-Item "$extractPath\arduino-cli.exe" -Destination $installPath -Force

# 5. Thêm vào PATH
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*ArduinoCLI*") {
    Write-Host "🔧 Adding to PATH..." -ForegroundColor Green
    [Environment]::SetEnvironmentVariable(
        "Path",
        $currentPath + ";$installPath",
        "User"
    )
}

# 6. Cleanup
Remove-Item $output -Force
Remove-Item $extractPath -Recurse -Force

Write-Host " Arduino CLI installed successfully!" -ForegroundColor Green
Write-Host "⚠️  Please RESTART your terminal to use arduino-cli" -ForegroundColor Yellow

# 7. Initialize Arduino CLI (trong terminal mới)
Write-Host ""
Write-Host "📋 Sau khi restart terminal, chạy các lệnh sau:" -ForegroundColor Cyan
Write-Host "  arduino-cli config init" -ForegroundColor White
Write-Host "  arduino-cli core update-index" -ForegroundColor White
Write-Host "  arduino-cli core install arduino:avr" -ForegroundColor White
