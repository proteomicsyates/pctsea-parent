get_cell_type_file <- function(unziped_files_folder, run_name, cell_type, file_suffix){
  folder <- unziped_files_folder
  # folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
  folder <- paste0(folder,  '/', "cell_types_charts")
  pattern <- paste0(run_name, "_", cell_type, "_", file_suffix, ".txt")
  file <- paste0(folder,"/",pattern)
  if(file.exists(file)){
    return (file)
  }
  list_files <- list.files(folder, pattern = pattern)
  print(list_files[1])
  if (length(list_files) > 0) {
    paste0(folder, '/', list_files[1])
  }else{
    return(NULL)
  }
}

get_global_file <- function(unziped_files_folder, run_name, file_suffix){
  folder <- unziped_files_folder
  # folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
  folder <- paste0(folder, .Platform$file.sep, "global_charts")
  list_files <- list.files(folder, pattern = paste0(run_name, "_.*", file_suffix, ".*.txt"))
  if (length(list_files) > 0) {
    paste0(folder, '/', list_files[1])
  }else{
    return(NULL)
  }
}

plot_axis_title_format <- list(font = list(size = 12))
