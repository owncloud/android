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

def count_files_containing_string(in_name, in_file):
    output = shell_run(f"""
        for dir in `find . -name "main" -execdir pwd \;`; do
            dir=$dir/main
            cd $dir
            grep -ril "{in_file}" | grep "{in_name}"
            true
        done | wc -l
        """).decode("utf-8")
    return int(output)


kotlin_lines = lines_of_filetype_in_folders("*.kt", "main")
java_lines = lines_of_filetype_in_folders("*.java", "main")
total_lines = java_lines + kotlin_lines

print(f"Java Lines: {round((java_lines/total_lines)*100)}%")
print(f"Kotlin Lines: {round((kotlin_lines/total_lines)*100)}%")

n_fragments = count_files_containing_string("Fragment", "")
n_activities = count_files_containing_string("Activity", "")
viewmodel_fragments = count_files_containing_string("Fragment", "iewModel")
viewmodel_activities = count_files_containing_string("Activity", "iewModel")
total_ui = n_fragments + n_activities
total_mvvm = viewmodel_fragments + viewmodel_activities

print(f"Total activities: {n_activities}, MVVM activities: {viewmodel_activities}")
print(f"Total fragments: {n_fragments}, MVVM fragments: {viewmodel_fragments}")
print(f"Total converted: {round((total_mvvm/total_ui)*100)}%")
