#!/usr/bin/python3
import subprocess
from datetime import date
from dateutil.relativedelta import relativedelta

def shell_run(command):
    return subprocess.check_output(['bash', '-c', command])


def lines_of_filetype_in_folders(filetype, foldername):
    lines_list = shell_run(f"""
        for dir in $(find . -name "{foldername}"); do
            find $dir -name "{filetype}" | xargs cat | wc -l;
        done
        """).decode("utf-8")
    total = 0
    for number in lines_list.strip().split("\n"):
        total += int(number)
    return total

def count_files_containing_string(in_name, in_file):
    output = shell_run(f"""
        for dir in $(find . -name "main" -execdir pwd \;); do
            dir=$dir/main
            cd $dir
            grep -ril "{in_file}" | grep "{in_name}"
            true
        done | wc -l
        """).decode("utf-8")
    return int(output)

def print_lines_stats():
    kotlin_lines = lines_of_filetype_in_folders("*.kt", "main")
    java_lines = lines_of_filetype_in_folders("*.java", "main")
    total_lines = java_lines + kotlin_lines

    print(f"Java Lines: {round((java_lines/total_lines)*100)}%")
    print(f"Kotlin Lines: {round((kotlin_lines/total_lines)*100)}%")


def print_mvvm_stats():
    n_fragments = count_files_containing_string("Fragment", "")
    n_activities = count_files_containing_string("Activity", "")
    viewmodel_fragments = count_files_containing_string("Fragment", "iewModel")
    viewmodel_activities = count_files_containing_string("Activity", "iewModel")
    total_ui = n_fragments + n_activities
    total_mvvm = viewmodel_fragments + viewmodel_activities

    print(f"Total activities: {n_activities}, MVVM activities: {viewmodel_activities}")
    print(f"Total fragments: {n_fragments}, MVVM fragments: {viewmodel_fragments}")
    print(f"Total converted: {round((total_mvvm/total_ui)*100)}%")

# print_lines_stats()
# print_mvvm_stats()

def get_list_of_last_commits_in_month():
    time_step = relativedelta(months=1)
    start_time = date.fromisocalendar(2019, 0o01, 0o01)
    time_pos = start_time
    list_of_last_commits = []
    for i in range(0, 2*12 + 8):
        time_after = str(time_pos).replace("-", ".")
        time_before = str(time_pos + time_step).replace("-", ".") 
        last_commit = shell_run(f"""
            git log --after {time_after} --before {time_before} \
                    | grep 'commit [0-9a-f]*$' \
                    | head -n 1
            """).decode("utf-8").strip().replace("commit ", "")
        list_of_last_commits.append((time_after, last_commit))
        time_pos += time_step
    return list_of_last_commits

def get_stats_from_commit_list(commit_list):
    for commit in commit_list:
        shell_run(f"""
            git checkout {commit[1]} 2> /dev/null && \
            git submodule update 2> /dev/null
        """)
        print(f"========== {commit[0]} ==========")
        print_lines_stats()
        print_mvvm_stats()
        print("")


get_stats_from_commit_list(get_list_of_last_commits_in_month())

