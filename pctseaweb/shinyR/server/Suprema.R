createPlotWithSupremaHistogram <- function(table){
  req(table)
  colnames(table) <- c('suprema')
  plot <- ggplot(data = table, aes(x=suprema, fill="red")) +
    geom_histogram() +
    labs(x = "suprema", y = "frequency") +
    theme_classic() +
    theme(legend.position = 'none')
  
 
  ggplotly(plot) %>%
    layout(
      title = list(
        text = 'Distribution of positive suprema positions',
        font = list(size = 12)
      ),
      xaxis = list(
        title = plot_axis_title_format,
        tickfont = list(size = 8),
        tickangle = 45
      ),
      yaxis = list(
        title = plot_axis_title_format
      )
    )
}



suprema_hist_table <- reactiveVal()
observeEvent(rv$suprema_histogram_file, {
  req(rv$suprema_histogram_file)
  table = fread(rv$suprema_histogram_file, header = FALSE, sep = "\t", showProgress = TRUE)
  suprema_hist_table(table)
})
observeEvent(suprema_hist_table(),{
  output$globalSupremaHistogramPlot <- renderPlotly(suprema_hist_table() %>% createPlotWithSupremaHistogram())
})


createPlotWithSupremaScatter <- function(table){
  req(table)
  colnames(table) <- c('cell type', 'suprema positions in ranked list', 'supremum size')
  plot <- ggplot(data = table, aes(x=`suprema positions in ranked list`, y=`supremum size`, color = `cell type`)) +
    geom_point(shape = 1) +
    labs(x = 'suprema positions in ranked list', y = 'supremum size') +
    theme_classic() +
    theme(legend.position = 'none')
  ggplotly(plot) %>%
    layout(
      xaxis = list(
        title = plot_axis_title_format
      ),
      yaxis = list(
        title = plot_axis_title_format
      )
    )
}

suprema_scatter_table <- reactiveVal()
observeEvent(rv$suprema_scatter_file, {
  req(rv$suprema_scatter_file)
  table = fread(rv$suprema_scatter_file, header = TRUE, sep = "\t", showProgress = TRUE)
  suprema_scatter_table(table)
})

observeEvent(suprema_scatter_table(),{
  output$globalSupremaScatterPlot <- renderPlotly(suprema_scatter_table() %>% createPlotWithSupremaScatter())
})
