createPlotWithCorrelations <- function(table, correlation_threshold, cell_type){
  req(table)
  browser()
  # create a new column that says whether the correlation pass the threshold or not
  positive <- "positive"
  table[, positive] <- table$pearsons_corr > correlation_threshold
  # remove correlations that are NaN
  table <- table[!is.na(table$pearsons_corr),]
  # create a rank column
  table[, "rank"] <- c(1:nrow(table))
  # title
  my_title <- "Ranks of cells by Pearson's correlation"
  # filter by cell type
  if(!missing(cell_type)){
    if(!sjmisc::is_empty(cell_type)){
      table <- table[table$Cell.type == cell_type,]
      my_title <- paste("Ranks of cells of type '",cell_type,  "' by Pearson's correlation", sep="")
    }else{
      return()
    }
  }
  ggplot(data = table,
         aes(
           x = rank,
           y = pearsons_corr,
           group = positive,
           fill = positive)) + # color by Comprador
    labs(title = my_title, x = "cell #", y = "Pearson's correlation") +
    geom_line(aes(color=positive)) +
    theme_classic()
}

createPlotWithCorrelationsForCellType <- function(table, cell_type, score_name){
  req(table, cell_type)
  names(table) <- c('cell type', score_name, 'Frequency (# cells)')
  plot <- ggplot(data = table,
         aes(
           x = get(score_name),
           y = `Frequency (# cells)`)) +
    labs(x = score_name, y = "Frequency (# cells)") +
    geom_line(aes(color = `cell type`))+
    theme_classic() +
    # xlim(-1,1) +
    theme(legend.position = 'none') # no legend

  ggplotly(plot) %>% layout(
    xaxis = list(
      title = plot_axis_title_format
    ),
    yaxis = list(
      title = plot_axis_title_format
    )
  )
}

# select the correlations file
# correlations_file <- eventReactive(rv$unziped_files,{
#   browser()
#   folder = rv$unziped_files
#   get_cell_type_file(unziped_files_folder = rv$unziped_files, run_name = rv$run_name, cell_type = input$selectCellType, file_suffix = "correlations")
#   paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*correlations.txt")[1], sep = "")
# })

cell_type_correlations_table <- eventReactive(input$selectCellType, {
  req(rv$unziped_files, input$selectCellType)
  file <- get_cell_type_file(rv$unziped_files, rv$run_name, input$selectCellType, "corr")
  if(is.null(file)){
    return()
  }
  table = fread(file, header = TRUE, sep = "\t", showProgress = TRUE)
  table
}, ignoreInit = TRUE)



# plot the cell_type correlation histogram plot
observeEvent(cell_type_correlations_table(),{
  table <- cell_type_correlations_table()
  score_name <- colnames(table)[2]
  output$cellTypeCorrelationsPlot <- renderPlotly(table %>% createPlotWithCorrelationsForCellType(., input$selectCellType, score_name))
})

# read the file
# rv$correlations_table <- eventReactive(correlations_file(),{
#   browser()
#   withProgress({
#     table = fread(file = correlations_file(), header = TRUE, sep = "\t", showProgress = TRUE)
#     setProgress(value = 1)
#     table
#   }, message = "Reading correlations file", detail = "This can take a few seconds. Please wait...")
# }, ignoreInit = TRUE)


# filteredCorrelationsTable <- eventReactive(rv$correlations_table,{
#   browser()
#   t2 <- rv$correlations_table
#   t2 <- t2[t2$pearsons_corr > 0.3,]
#
#   # plot the table as soon as is loaded
#   output$correlationsDataTable <- DT::renderDataTable({
#     t <- filteredCorrelationsTable()
#   }, options = list(pageLength = 20)
#   )
# })


# observeEvent(input$selectCellType, {
#   req(input$selectCellType)
#   t <- rv$correlations_table
#   req(t)
#   browser()
#   t <- t[t$cell_type == input$selectCellType,]
#   rv$correlations_table <- t
# })

# plot the correlation plot
# observeEvent(rv$correlations_table,{
#   browser()
#   output$correlationsPlot <- renderPlot(rv$correlations_table %>% createPlotWithCorrelations(., 0.1))
# })
