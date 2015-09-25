#!/usr/bin/python

import sys
import subprocess
import os

def main():
    pids = []

    amounts = []
    for line in open("launcherOptions"):
        line = line.strip("\n")
        arg_amount_list = line.split(":")
        amounts.append(arg_amount_list[1])

    print(amounts)

    count = 0
    for line in open("launcherScripts"):
        line = line.strip("\n")
        arg_list = line.split(" ")

        qty = amounts[count]
        numericQty = int(qty)
        for i in range(0, numericQty):
            process = subprocess.Popen(arg_list, shell=False)
            pids.append(str(process.pid))

        count += 1

    prompt = "Write Q\n"
    while raw_input(prompt) != "Q":
        continue

    for pid in pids:
        print("Killing process with pid " + pid)
        os.system("kill -2 " + pid)


if __name__ == '__main__':
    main()


