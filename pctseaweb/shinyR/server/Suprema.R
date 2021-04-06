createPlotWithSupremaHistogram <- function(table){
  req(table)

  colnames(table) <- c('cell type', 'supremum_X')
  plot <- plot_ly(table, alpha = 0.7, x =~supremum_X, type = 'histogram') %>%
  layout(
    xaxis = list(title = 'Supremum X'),
    yaxis = list(title = 'Frequency'),
    title = list(
      text = 'Distribution of positive suprema positions across cell types',
      font = list(size = 11)
    )
  )
  # plot <- ggplot(data = table, aes(x=`supremum X`, fill="red")) +
  #   geom_histogram() +
  #   labs(x = "supremum X", y = "frequency") +
  #   theme_classic() +
  #   theme(legend.position = 'none')
  #
  #
  # ggplotly(plot) %>%
  #   layout(
  #     title = list(
  #       text = 'Distribution of positive suprema positions across cell types',
  #       font = list(size = 12)
  #     ),
  #     xaxis = list(
  #       title = plot_axis_title_format,
  #       tickfont = list(size = 8),
  #       tickangle = 45
  #     ),
  #     yaxis = list(
  #       title = plot_axis_title_format
  #     )
  #   )
}



suprema_hist_table <- reactiveVal()
observeEvent(rv$suprema_histogram_file, {
  req(rv$suprema_histogram_file)
  table = fread(rv$suprema_histogram_file, header = TRUE, sep = "\t", showProgress = TRUE)
  suprema_hist_table(table)
})
observeEvent(suprema_hist_table(),{
  output$globalSupremaHistogramPlot <- renderPlotly(suprema_hist_table() %>% createPlotWithSupremaHistogram())
})


createPlotWithSupremaScatter <- function(table){
  req(table)
  # colnames(table) <- c('cell type', 'suprema positions in ranked list', 'supremum size')
  colnames(table) <- c('cell_type', 'suprema_x', 'suprema_y')
  plot <- plot_ly(table, x =~suprema_x, y=~suprema_y, color=~cell_type, type = 'scatter', mode = 'markers') %>%
    layout(
      xaxis = list(title = 'suprema positions in ranked list'),
      yaxis = list(title = 'supremum size'),
      title = list(
        text = "Suprema values vs suprema positions per cell type",
        font = list(size = 11)
      )
    )
  # plot <- ggplot(data = table, aes(x=`suprema positions in ranked list`, y=`supremum size`, color = `cell type`)) +
  #   geom_point(shape = 1) +
  #   labs(x = 'suprema positions in ranked list', y = 'supremum size') +
  #   theme_classic() +
  #   theme(legend.position = 'none')
  # ggplotly(plot) %>%
  #   layout(
  #     xaxis = list(
  #       title = plot_axis_title_format
  #     ),
  #     yaxis = list(
  #       title = plot_axis_title_format
  #     ),
  #     title = list(
  #       text = "Suprema values vs suprema positions per cell type",
  #       font = list(size = 11)
  #     )
  #   )
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
