

# select the correlations file
correlations_file <- eventReactive(rv$unziped_files,{
  folder = rv$unziped_files
  paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*correlations.txt")[1], sep = "")
})
# read the file
correlations_table <- eventReactive(correlations_file(),{
  withProgress({
    table = fread(file = correlations_file(), header = TRUE, sep = "\t", showProgress = TRUE)
    setProgress(value = 1)
    table
  }, message = "Reading correlations file", detail = "This can take a few seconds. Please wait...")
})

filteredCorrelationsTable <- eventReactive(correlations_table(),{
  browser()
  t <- correlations_table()
  t <- t[t$pearsons_corr > 0.3,]
})
# plot the table as soon as is loaded
output$correlationsDataTable <- DT::renderDataTable({
  t <- filteredCorrelationsTable()
}, options = list(pageLength = 20)
)
# plot the correlation plot
output$correlationsPlot <- renderPlot(correlations_table() %>% createPlotWithCorrelations(., 0.1))
