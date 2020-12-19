createPlotWithUmap <- function(table, title){
  req(table)
  colnames(table) <- c('cell type', 'x', 'y')
  plot <- ggplot(data = table, aes(x=x, y=y, color = `cell type`)) +
    geom_point(shape = 1) +
    labs(x = 'UMAP x', y = 'UMAP y') +
    theme_classic() +
    theme(legend.position = 'none')
  ggplotly(plot) %>%
    layout(
      xaxis = list(
        title = plot_axis_title_format
      ),
      yaxis = list(
        title = plot_axis_title_format
      ),
      title = list(
        text = title,
        font = list(size = 10)
      )

    )
}


# umap_all_file = NULL,
# umap_hypG_file = NULL,
# umap_KStest_file = NULL,
# umap_sig001_file = NULL,
# umap_sig005_file = NULL

umap_all_table <- reactiveVal()
observeEvent(rv$umap_all_file, {
  req(rv$umap_all_file)
  table = fread(rv$umap_all_file, header = FALSE, sep = "\t", showProgress = TRUE)
  umap_all_table(table)
})
observeEvent(umap_all_table(),{
  output$umapAllPlot <- renderPlotly(umap_all_table() %>% createPlotWithUmap(title = 'UMAP clustering of all cell types<br> (no sig threshold)'))
})

umap_hypG_table <- reactiveVal()
observeEvent(rv$umap_hypG_file, {
  req(rv$umap_hypG_file)
  table = fread(rv$umap_hypG_file, header = FALSE, sep = "\t", showProgress = TRUE)
  umap_hypG_table(table)
})
observeEvent(umap_hypG_table(),{
  output$umapHypGPlot <- renderPlotly(umap_hypG_table() %>% createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> by hypergeometric test (p<0.05)'))
})

umap_KStest_table <- reactiveVal()
observeEvent(rv$umap_KStest_file, {
  req(rv$umap_KStest_file)
  table = fread(rv$umap_KStest_file, header = FALSE, sep = "\t", showProgress = TRUE)
  umap_KStest_table(table)
})
observeEvent(umap_KStest_table(),{
  output$umapKSTestPlot <- renderPlotly(umap_KStest_table() %>% createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> by K-S test'))
})

umap_sig001_table <- reactiveVal()
observeEvent(rv$umap_sig001_file, {
  req(rv$umap_sig001_file)
  table = fread(rv$umap_sig001_file, header = FALSE, sep = "\t", showProgress = TRUE)
  umap_sig001_table(table)
})
observeEvent(umap_sig001_table(),{
  output$umapSig001Plot <- renderPlotly(umap_sig001_table() %>% createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> (sig<0.01)'))
})

umap_sig005_table <- reactiveVal()
observeEvent(rv$umap_sig005_file, {
  req(rv$umap_sig005_file)
  table = fread(rv$umap_sig005_file, header = FALSE, sep = "\t", showProgress = TRUE)
  umap_sig005_table(table)
})
observeEvent(umap_sig005_table(),{
  output$umapSig005Plot <- renderPlotly(umap_sig005_table() %>% createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> (sig<0.05)'))
})


