createPlotWithGenesPerCellType <- function(table, cell_type, corr_threshold){
  req(table)
  plot <- ggplot(data = table) +
    geom_bar(stat="identity", aes(x=factor(V2), y=V3, color = V1, fill = V1), position = 'dodge') +
    labs(x = "# of genes corr > threshold", y = "# cells") +
    theme_classic() +
    theme(plot.title = element_text(size=10)) +
    theme(legend.position="bottom")
  ggplotly(plot) %>%
    layout(
      legend = list(
        orientation = "h",
        x = 0,
        y = 1.1,
        title = NULL
      )
    )
}


genes_table <- eventReactive(input$selectCellType, {
  req(rv$unziped_files, input$selectCellType)
  file <- get_cell_type_file(rv$unziped_files, rv$run_name, input$selectCellType, "genes_per_cell_hist")
  if(is.null(file)){
    return()
  }
  table = fread(file, header = FALSE, sep = "\t", showProgress = TRUE)
  table
}, ignoreInit = TRUE)



# plot the genes per cell type histogram plot
observeEvent(genes_table(),{
  output$genesPerCellTypePlot <- renderPlotly(genes_table() %>% createPlotWithGenesPerCellType(., input$selectCellType))
})

