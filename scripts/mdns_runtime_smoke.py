"""Run the opt-in desktop mDNS probe in two independent JVMs on the current LAN."""
import argparse
import shutil
import subprocess
import sys
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath", type=Path, required=True)
    args = parser.parse_args()
    command = [shutil.which("java") or "java", "-cp", args.classpath.read_text(encoding="utf-8"),
               "com.shterneregen.securelan.desktop.network.discovery.MdnsRuntimeProbe"]
    options = {"stdout": subprocess.PIPE, "stderr": subprocess.PIPE, "text": True}
    if sys.platform == "win32":
        options["creationflags"] = subprocess.CREATE_NO_WINDOW
    browser = subprocess.Popen(command + ["browse"], **options)
    host = subprocess.Popen(command + ["host"], **options)
    try:
        host_out, host_err = host.communicate(timeout=50)
        browse_out, browse_err = browser.communicate(timeout=55)
        print("host:", host_out.strip())
        print("browser:", browse_out.strip())
        if host.returncode or browser.returncode:
            # Probe code contains synthetic metadata only; no application user payloads.
            print(host_err, browse_err, file=sys.stderr)
            return 1
        return 0
    finally:
        for process in (browser, host):
            if process.poll() is None:
                process.kill()
                process.wait()


if __name__ == "__main__":
    raise SystemExit(main())
