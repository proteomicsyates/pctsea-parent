

createPlotWithCorrelationsForCellType <- function(table, cell_type ){
  req(table)

  score_name <- colnames(table)[1]
  num_cells <- nrow(table)
  plot <- plot_ly(table, x =~get(score_name), type = 'histogram') %>%
    layout(
      xaxis = list(titlefont = list(size = 12), title = score_name),
      yaxis = list(titlefont = list(size = 12), title = "Frequency (# cells)"),
      title = list(
        text = paste0("Distribution of ", score_name, " across ", num_cells, " cells of type '", cell_type, "'"),
        font = list(size = 11)
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
  output$cellTypeCorrelationsPlot <- renderPlotly(table %>% createPlotWithCorrelationsForCellType(., input$selectCellType))
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
