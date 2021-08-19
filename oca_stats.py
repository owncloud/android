#!/usr/bin/python3

import subprocess

def shell_run(command):
    return subprocess.check_output(['sh', '-c', command])


def lines_of_filetype_in_folders(filetype, foldername):
    lines_list = shell_run(f"""
        for dir in `find . -name "{foldername}"`; do
            find $dir -name "{filetype}" | xargs cat | wc -l;
        done
        """).decode("utf-8")
    total = 0
    for number in lines_list.strip().split("\n"):
        total += int(number)
    return total


kotlin_lines = lines_of_filetype_in_folders("*.kt", "main")
java_lines = lines_of_filetype_in_folders("*.java", "main")
total_lines = java_lines + kotlin_lines

print(f"Java Lines: {round((java_lines/total_lines)*100)}%")
print(f"Kotlin Lines: {round((kotlin_lines/total_lines)*100)}%")
