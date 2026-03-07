param(
    [string]$TargetDir = "$PSScriptRoot"
)

$code = @"
using System;
using System.Runtime.InteropServices;
using System.Collections.Generic;

public class FileLockUtil {
    [StructLayout(LayoutKind.Sequential)]
    struct RM_UNIQUE_PROCESS {
        public int dwProcessId;
        public System.Runtime.InteropServices.ComTypes.FILETIME ProcessStartTime;
    }
    
    [DllImport("rstrtmgr.dll", CharSet = CharSet.Auto)]
    static extern int RmStartSession(out uint pSessionHandle, int dwSessionFlags, string strSessionKey);
    
    [DllImport("rstrtmgr.dll")]
    static extern int RmEndSession(uint pSessionHandle);
    
    [DllImport("rstrtmgr.dll", CharSet = CharSet.Auto)]
    static extern int RmRegisterResources(uint pSessionHandle, uint nFiles, string[] rgsFilenames, uint nApplications, [In] RM_UNIQUE_PROCESS[] rgApplications, uint nServices, string[] rgsServiceNames);
    
    [DllImport("rstrtmgr.dll")]
    static extern int RmGetList(uint dwSessionHandle, out uint pnProcInfoNeeded, ref uint pnProcInfo, [In, Out] byte[] rgAffectedApps, out uint lpdwRebootReasons);

    public static int[] GetLockingPids(string filePath) {
        uint handle;
        string key = Guid.NewGuid().ToString();
        int res = RmStartSession(out handle, 0, key);
        if (res != 0) return new int[0];
        try {
            res = RmRegisterResources(handle, 1, new string[] { filePath }, 0, null, 0, null);
            if (res != 0) return new int[0];
            uint pnProcInfoNeeded = 0, pnProcInfo = 0, lpdwRebootReasons = 0;
            res = RmGetList(handle, out pnProcInfoNeeded, ref pnProcInfo, null, out lpdwRebootReasons);
            if (res == 234 && pnProcInfoNeeded > 0) {
                int structSize = Marshal.SizeOf(typeof(RM_UNIQUE_PROCESS)) + 256*2 + 64*2 + 4 + 4 + 4 + 4 + 4;
                byte[] processInfo = new byte[pnProcInfoNeeded * structSize];
                pnProcInfo = pnProcInfoNeeded;
                res = RmGetList(handle, out pnProcInfoNeeded, ref pnProcInfo, processInfo, out lpdwRebootReasons);
                if (res == 0) {
                    List<int> pids = new List<int>();
                    for (int i = 0; i < pnProcInfo; i++) {
                        int pid = BitConverter.ToInt32(processInfo, i * structSize);
                        pids.Add(pid);
                    }
                    return pids.ToArray();
                }
            }
        } finally {
            RmEndSession(handle);
        }
        return new int[0];
    }
}
"@

if (-not ([System.Management.Automation.PSTypeName]'FileLockUtil' -as [type])) {
    Add-Type -TypeDefinition $code -IgnoreWarnings
}

$buildDirs = Get-ChildItem -Path $TargetDir -Directory -Recurse | Where-Object { $_.FullName -match "\\build(\\|\$)" }
$jars = @()
foreach ($dir in $buildDirs) {
    # Scan for ANY files in build dirs that Android usually locks (.jar, .apk, .dex)
    $jars += Get-ChildItem -Path $dir.FullName -Recurse -Include *.jar, *.apk, *.dex, *.lock -ErrorAction SilentlyContinue
}

$killedPids = @()
$ignoredProcs = @("explorer", "studio64", "powershell", "cmd", "pwsh", "idea64")

foreach ($jar in $jars) {
    try {
        $stream = [System.IO.File]::Open($jar.FullName, 'Open', 'Write', 'None')
        $stream.Close()
    }
    catch {
        # File is locked!
        $pids = [FileLockUtil]::GetLockingPids($jar.FullName)
        foreach ($p in $pids) {
            if ($p -notin $killedPids) {
                $proc = Get-Process -Id $p -ErrorAction SilentlyContinue
                if ($proc -and ($proc.Name -notin $ignoredProcs)) {
                    Write-Host "[Auto-Unlock] Killing PID $p ($($proc.Name)) locking $($jar.Name)" -ForegroundColor Yellow
                    Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
                    $killedPids += $p
                }
            }
        }
    }
}
