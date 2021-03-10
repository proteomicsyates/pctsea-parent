

createPlotWithGlobalCorrelationsNEW <- function(table, score_name){
  req(table)
  names(table) <- c('ax', score_name, 'Frequency (# cells)')
  plot <- ggplot(data = table,
                 aes(
                   x = get(score_name),
                   y = `Frequency (# cells)`)) +
    labs(x = score_name, y = "Frequency (# cells)") +
    geom_line(aes(color = ax))+
    theme_classic() +
    # xlim(-1,1) +
    theme(legend.position = 'none') # no legend
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
createPlotWithGlobalRankCorrelationsNEW <- function(table, score_name){
  req(table)
  names(table) <- c('x', 'Rank', score_name)
  plot <- ggplot(data = table,
                 aes(
                   x = Rank,
                   y = get(score_name))) +
    labs(x = "ranked cells", y = score_name) +
    geom_line(aes(color = x))+
    theme_classic() +
    theme(legend.title = element_blank())
  # ggtitle(paste0("Corr. distrib. for: '",cell_type, "'")) +
  # theme(plot.title = element_text(size=10))
  ggplotly(plot) %>%
    layout(
      legend = list(
        orientation = "v",
        x = 0.5,
        y = 0.6,
        title = NULL
      ),
      xaxis = list(
        title = plot_axis_title_format
      ),
      yaxis = list(
        title = plot_axis_title_format
      )
    )
}

global_correlations_table <- reactiveVal()
observeEvent(rv$global_correlations_file, {
  req(rv$global_correlations_file)
  table = fread(rv$global_correlations_file, header = TRUE, sep = "\t", showProgress = TRUE)
  global_correlations_table(table)
})
global_correlations_rank_table <- reactiveVal()
observeEvent(rv$global_correlations_rank_file, {
  req(rv$global_correlations_rank_file)
  table = fread(rv$global_correlations_rank_file, header = TRUE, sep = "\t", showProgress = TRUE)
  global_correlations_rank_table(table)
})


# plot the global correlation histogram plot
observeEvent(global_correlations_table(),{
  table <- global_correlations_table()
  score_name <- colnames(table)[2]
  output$globalCorrelationsPlot <- renderPlotly(table %>% createPlotWithGlobalCorrelationsNEW(., score_name))
})


# plot the global rank correlation histogram plot
observeEvent(global_correlations_rank_table(),{
  table <- global_correlations_rank_table()
  score_name <- colnames(table)[3]
  output$globalCorrelationsRankPlot <- renderPlotly(table %>% createPlotWithGlobalRankCorrelationsNEW(., score_name))
})


