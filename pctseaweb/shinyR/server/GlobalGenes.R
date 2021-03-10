createPlotWithGlobalGenesPerCellType <- function(table, score_name){
  req(table)
  colnames(table) <- c('type', '# genes', '# cells')
  plot <- ggplot(data = table) +
    geom_bar(stat="identity", aes(x=factor(`# genes`), y=`# cells`, fill = type), position = 'dodge') +
    labs(x = paste("# of genes with", score_name, "> threshold"), y = "# cells") +
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
      xaxis = list(
        title = plot_axis_title_format,
        tickfont = list(size = 8)
      ),
      yaxis = list(
        title = plot_axis_title_format
      )
    )
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

