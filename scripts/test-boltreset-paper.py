#!/usr/bin/env python3
"""隔離したPaperサーバーで、保護の削除・拒否条件・再起動後の保存を確かめる。

server-data/ に Paper の JAR（paper-26.2-129.jar）と plugins/Bolt-1.2.22.jar を置いておく。
先に `mvn -B package` を実行しておく（target/test-classes を使う）。
1回目の起動で削除と拒否条件を確かめ、2回目の起動で削除が保存されたことを確かめる。
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys
import threading
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "server-data"
WORK = ROOT / "target/boltreset-paper-smoke"
JAVA = os.environ.get("JAVA_BIN", "/opt/homebrew/opt/openjdk/bin/java")
PAPER = os.environ.get("PAPER_JAR", "paper-26.2-129.jar")
MARKERS = ("BOLT_PROBE_PHASE1_PASS", "BOLT_PROBE_PASS", "BOLT_PROBE_FAIL")


def prepare():
    if WORK.exists():
        shutil.rmtree(WORK)
    plugins = WORK / "plugins"
    (plugins / "BoltReset").mkdir(parents=True)
    for source, target in (
        (SOURCE / PAPER, WORK / "paper.jar"),
        (SOURCE / "plugins/Bolt-1.2.22.jar", plugins / "Bolt.jar"),
        (ROOT / "target/bolt-reset-1.0.0.jar", plugins / "BoltReset.jar"),
    ):
        shutil.copy2(source, target)
    (plugins / "BoltReset/config.yml").write_text(
        "worlds: [reset_a, reset_end]\nrequire-whitelist: true\nrequire-empty-server: true\n"
    )
    with zipfile.ZipFile(plugins / "BoltResetProbe.jar", "w") as jar:
        jar.writestr("plugin.yml", "name: BoltResetProbe\nversion: 1\n"
                     "main: io.github.spa77k.boltreset.BoltResetProbe\n"
                     "api-version: '1.20'\ndepend: [Bolt, BoltReset]\n")
        for source in (ROOT / "target/test-classes/io/github/spa77k/boltreset").glob("BoltResetProbe*.class"):
            jar.write(source, "io/github/spa77k/boltreset/" + source.name)
    (WORK / "eula.txt").write_text("eula=true\n")
    (WORK / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25589\nonline-mode=false\n"
        "spawn-protection=0\nmax-players=1\nlevel-type=minecraft:flat\n"
        # 26.3から新規サーバーの既定値がtrueなので、オフの状態から始める
        "white-list=false\n"
    )


def run_server():
    process = subprocess.Popen(
        [JAVA, "-Xms512M", "-Xmx1G", "-jar", "paper.jar", "--nogui"],
        cwd=WORK, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, text=True, bufsize=1,
    )
    lines = []
    done = threading.Event()

    def read_output():
        for line in process.stdout:
            lines.append(line)
            if any(marker in line for marker in MARKERS):
                done.set()

    threading.Thread(target=read_output, daemon=True).start()
    try:
        if not done.wait(240):
            raise RuntimeError("Paperの検証が240秒以内に終わりませんでした")
    finally:
        try:
            process.wait(60)
        except subprocess.TimeoutExpired:
            process.kill()
    for line in lines:
        if any(word in line for word in ("ok: ", "[BoltReset]", "BOLT_PROBE", "Exception", "Error", "\tat ")):
            print(line.rstrip())
    return "".join(lines)


def main():
    prepare()
    if "BOLT_PROBE_PHASE1_PASS" not in run_server():
        sys.exit(1)
    if "BOLT_PROBE_PASS" not in run_server():
        sys.exit(1)


if __name__ == "__main__":
    main()
