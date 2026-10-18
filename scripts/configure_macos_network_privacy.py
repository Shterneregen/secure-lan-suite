"""Add room-discovery privacy declarations to an existing jpackage application."""
import argparse
import plistlib
from pathlib import Path


def configure(path: Path) -> None:
    with path.open("rb") as source:
        value = plistlib.load(source)
    value["NSLocalNetworkUsageDescription"] = (
        "SecureLan discovers and hosts password-protected rooms on your local network."
    )
    value["NSBonjourServices"] = ["_securelan._tcp"]
    with path.open("wb") as output:
        plistlib.dump(value, output, sort_keys=False)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("plist", type=Path)
    configure(parser.parse_args().plist)
