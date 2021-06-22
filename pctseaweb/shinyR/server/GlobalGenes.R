createPlotWithGlobalGenesPerCellType <- function(table, score_name){
  req(table)
  colnames(table) <- c('type', 'genes', 'cells')
  plot_ly(table, x =~genes, y=~cells, type = 'bar', split=~type ) %>%
    layout(
      xaxis = list(titlefont = list(size = 12), title = "# genes"),
      yaxis = list(titlefont = list(size = 12), title = "# cells"),
      title = list(
        text = paste0("Number of cells that express\neach number of genes (or more)"),
        font = list(size = 11)
      ),
      legend = list(x = 0.5, y = 1)
    )

  # plot <- ggplot(data = table) +
  #   geom_bar(stat="identity", aes(x=factor(`# genes`), y=`# cells`, fill = type), position = 'dodge') +
  #   labs(x = score_name, y = "# cells") +
  #   theme_classic() +
  #   theme(legend.title = element_blank())
  # ggplotly(plot) %>%
  #   layout(
  #     legend = list(
  #       x = 0.5,
  #       y = 0.6,
  #       title = NULL,
  #       font = list(size = 9),
  #       tracegroupgap = 3
  #     ),
  #     xaxis = list(
  #       title = plot_axis_title_format,
  #       tickfont = list(size = 8)
  #     ),
  #     yaxis = list(
  #       title = plot_axis_title_format
  #     ),
  #     title = list(
  #       text = paste0("# cells that express each number of genes (or more)"),
  #       font = list(size = 11)
  #     )
  #   )
}
global_genes_table <- reactiveVal()
observeEvent(rv$global_genes_file, {
  req(rv$global_genes_file)
  table = fread(rv$global_genes_file, header = TRUE, sep = "\t", showProgress = TRUE)
  global_genes_table(table)
})




# plot the genes per cell type histogram plot
observeEvent(global_genes_table(),{
  table <- global_genes_table()
  score_name <- colnames(table)[2]
  output$globalGenesPerCellTypePlot <- renderPlotly(table %>% createPlotWithGlobalGenesPerCellType(score_name))
})

