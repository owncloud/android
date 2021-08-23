#!/usr/bin/python3
import subprocess
from datetime import date
from dateutil.relativedelta import relativedelta
import pandas as pd
import sys

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

def get_lines_stats():
    kotlin_lines = lines_of_filetype_in_folders("*.kt", "main")
    java_lines = lines_of_filetype_in_folders("*.java", "main")
  
    return {"jlines": java_lines,
            "klines": kotlin_lines}

def get_mvvm_stats():
    n_fragments = count_files_containing_string("Fragment", "")
    n_activities = count_files_containing_string("Activity", "")
    viewmodel_fragments = count_files_containing_string("Fragment", "iewModel")
    viewmodel_activities = count_files_containing_string("Activity", "iewModel")

    return {"n_fragments": n_fragments,
            "n_activities": n_activities,
            "vm_fragments": viewmodel_fragments,
            "vm_activities": viewmodel_activities}


def get_list_of_last_commits_in_month():
    time_step = relativedelta(months=1)
    start_time = date.fromisocalendar(2019, 0o01, 0o02)
    time_pos = start_time
    list_of_last_commits = []
    for i in range(0, 2*12 + 8):
        time_after = str(time_pos - time_step).replace("-", ".")
        time_before = str(time_pos).replace("-", ".") 
        last_commit = shell_run(f"""
            git log --after {time_after} --before {time_before} \
                    | grep 'commit [0-9a-f]*$' \
                    | head -n 1
            """).decode("utf-8").strip().replace("commit ", "")
        list_of_last_commits.append((time_before, last_commit))
        time_pos += time_step
    return list_of_last_commits

def get_stats_from_commit_list(commit_list):
    stats = {
                "date": [],
                "commit": [],
                "jlines": [],
                "klines": [],
                "n_fragments": [],
                "n_activities": [],
                "vm_fragments": [],
                "vm_activities": []
            }
    for commit in commit_list:
        print(f"Process: {commit[0]} ")

        shell_run(f"""
            git checkout {commit[1]} 2> /dev/null && \
            git submodule update 2> /dev/null
        """)
        cstats = get_lines_stats()
        cstats.update(get_mvvm_stats())
        
        def add_to_stats(val):
            stats[val].append(cstats[val])

        stats["date"].append(str(commit[0]))
        stats["commit"].append(commit[1])
        add_to_stats("jlines")
        add_to_stats("klines")
        add_to_stats("n_fragments")
        add_to_stats("n_activities")
        add_to_stats("vm_fragments")
        add_to_stats("vm_activities")
    return stats


def main():
    if len(sys.argv) < 2:
        print("Please enter filename for the csv to export")
        print("CAUTION: Do not store csv in the git repo directory!!!")
        quit(1)
    csv_file_name = sys.argv[1]
    stats = get_stats_from_commit_list(get_list_of_last_commits_in_month())
    df = pd.DataFrame(stats)
    df.to_csv(csv_file_name, index=False)


if __name__ == "__main__":
    main()
