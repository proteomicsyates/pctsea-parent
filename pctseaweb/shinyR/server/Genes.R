createPlotWithGenesPerCellType <- function(table, cell_type, corr_threshold){
  req(table)
  colnames(table) <- c('type', '# genes', '# cells')
  plot <- ggplot(data = table) +
    geom_bar(stat="identity", aes(x=factor(`# genes`), y=`# cells`, fill = type), position = 'dodge') +
    labs(x = "# of genes with corr > threshold", y = "# cells") +
    theme_classic() +
    theme(legend.title = element_blank())
  ggplotly(plot) %>%
    layout(
      legend = list(
        x = 0.5,
        y = 0.6,
        title = NULL,
        font = list(size = 9),
        tracegroupgap = 3
      ),
      xaxis = list(title = plot_axis_title_format),
      yaxis = list(title = plot_axis_title_format)
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
