#!/usr/bin/python

import sys
import subprocess
import os

def main():
    exec_file = sys.argv[1]
    pids = []

    for line in open(exec_file):
        line = line.strip("\n")
        print line
        arg_list = line.split(" ")
        print arg_list
        process = subprocess.Popen(arg_list, shell=False)
        pids.append(str(process.pid))

    prompt = "Write MONDONGO to stop processes\n"
    while raw_input(prompt) != "MONDONGO":
        continue

    for pid in pids:
        print "Killing process with pid " + pid
        os.system("kill -2 " + pid)


if __name__ == '__main__':
    main()


