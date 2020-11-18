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

# select the correlations file
correlations_file <- eventReactive(unziped_files(),{
  browser()
  folder = unziped_files()
  paste(folder, .Platform$file.sep, list.files(folder, pattern = ".*correlations.txt")[1], sep = "")
})

# read the file
rv <- reactiveValues(correlations_table = NULL)
rv$correlations_table <- eventReactive(correlations_file(),{
  browser()
  withProgress({
    table = fread(file = correlations_file(), header = TRUE, sep = "\t", showProgress = TRUE)
    setProgress(value = 1)
    table
  }, message = "Reading correlations file", detail = "This can take a few seconds. Please wait...")
})


filteredCorrelationsTable <- eventReactive(rv$correlations_table,{
  browser()
  t2 <- rv$correlations_table
  t2 <- t2[t2$pearsons_corr > 0.3,]

  # plot the table as soon as is loaded
  output$correlationsDataTable <- DT::renderDataTable({
    t <- filteredCorrelationsTable()
  }, options = list(pageLength = 20)
  )
})


observeEvent(input$selectCellType, {
  req(input$selectCellType)
  t <- rv$correlations_table
  req(t)
  browser()
  t <- t[t$cell_type == input$selectCellType,]
  rv$correlations_table <- t
})

# plot the correlation plot
observeEvent(rv$correlations_table,{
  browser()
  output$correlationsPlot <- renderPlot(rv$correlations_table %>% createPlotWithCorrelations(., 0.1))
})
