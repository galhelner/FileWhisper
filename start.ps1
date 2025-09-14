# Start docker-compose in detached mode
docker-compose up -d

# Wait until localhost:3000 responds with HTTP 200
Write-Host "Waiting for frontend to be fully ready..."
do {
    Start-Sleep -Seconds 2
    $response = try {
        $r = Invoke-WebRequest -UseBasicParsing http://localhost:3000 -ErrorAction Stop
        if ($r.StatusCode -eq 200) { $true } else { $false }
    } catch {
        $false
    }
} while (-not $response)

# Optional: small buffer to ensure full readiness
Start-Sleep -Seconds 2

# Open default browser
Start-Process "http://localhost:3000"
