get_cell_type_file <- function(unziped_files_folder, run_name, cell_type, file_suffix){
  folder <- unziped_files_folder
  folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
  folder <- paste0(folder, .Platform$file.sep, "cell_types_charts")
  list_files <- list.files(folder, pattern = paste0(run_name, "_.*", cell_type, ".*", file_suffix, ".txt"))
  if (length(list_files) > 0) {
    paste0(folder, .Platform$file.sep, list_files[1])
  }else{
    return(NULL)
  }
}

get_global_file <- function(unziped_files_folder, run_name, file_suffix){
  folder <- unziped_files_folder
  folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
  folder <- paste0(folder, .Platform$file.sep, "global_charts")
  list_files <- list.files(folder, pattern = paste0(run_name, "_.*", file_suffix, ".*.txt"))
  if (length(list_files) > 0) {
    paste0(folder, .Platform$file.sep, list_files[1])
  }else{
    return(NULL)
  }
}