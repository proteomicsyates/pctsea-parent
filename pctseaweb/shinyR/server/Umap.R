createPlotWithUmap <- function(table, title, dimensions){
  req(dimensions)
  if (dimensions=="2D"){
    createPlotWithUmap2D(table, title)
  }else if (dimensions=="3D"){
    createPlotWithUmap3D(table, title)
  }else if (dimensions=="4D"){
    createPlotWithUmap4D(table, title)
  }
}

createPlotWithUmap3D <- function(table, title){
  colnames(table)[1] <- 'cell type'
  colnames(table)[2] <- 'UMAP1'
  colnames(table)[3] <- 'UMAP2'
  colnames(table)[4] <- 'UMAP3'
  colnames(table)[5] <- 'UMAP4'
  scene = list(
    xaxis = axis3d("UMAP1"),
    yaxis = axis3d("UMAP2"),
    zaxis = axis3d("UMAP3")
  )
  table %>% plot_ly(x = ~UMAP1, y = ~UMAP2, z = ~UMAP3, text = ~`cell type`, color =  ~`cell type`) %>% add_markers(size=3) %>% hide_legend() %>%
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
      ),
      scene = scene

    )
}

axis3d <- function(name){
  list(
    title = name,
    titlefont = list(size = 10),
    tickfont = list(size = 10)
  )
}
createPlotWithUmap4D <- function(table, title){
  colnames(table)[1] <- 'cell type'
  colnames(table)[2] <- 'UMAP1'
  colnames(table)[3] <- 'UMAP2'
  colnames(table)[4] <- 'UMAP3'
  colnames(table)[5] <- 'UMAP4'

  scene = list(
    xaxis = axis3d("UMAP1"),
    yaxis = axis3d("UMAP2"),
    zaxis = axis3d("UMAP3")
  )
  table %>% plot_ly(x = ~UMAP1, y = ~UMAP2, z = ~UMAP3, text = ~`cell type`, color =  ~UMAP4) %>%
    add_markers(size=3) %>%
    hide_legend() %>%
    layout(
      scene = scene,
      title = list(
        text = title,
        font = list(size = 10)
      )

    )
}
createPlotWithUmap2D <- function(table, title){
  req(table)
  colnames(table)[1] <- 'cell type'
  colnames(table)[2] <- 'x'
  colnames(table)[3] <- 'y'
  showLabels <- input$showLabels
  plot <- ggplot(data = table, aes(x = x, y = y, color = `cell type`, label = `cell type`)) +
    geom_point(shape = 1) +
    labs(x = 'UMAP 1', y = 'UMAP 2') +
    theme_classic() +
    theme(legend.position = 'none')
  if(showLabels){
    plot <- plot + geom_text(size=3, aes(x = x, y = y, label = `cell type`),  nudge_y = 0.2)
  }
  ploty <- ggplotly(plot) %>%
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
  # if(showLabels){
  #   ploty %>% add_text(text = '', textposition = "top right")
  # }
  ploty
}


# umap_all_file = NULL,
# umap_hypG_file = NULL,
# umap_KStest_file = NULL,
# umap_sig001_file = NULL,
# umap_sig005_file = NULL

umap_all_table <- reactiveVal()
observeEvent(rv$umap_all_file, {
  req(rv$umap_all_file)
  print(paste("Reading file ", rv$umap_all_file))
  table = fread(rv$umap_all_file, header = TRUE, sep = "\t", showProgress = TRUE)
  umap_all_table(table)
})
observeEvent(
  eventExpr = {
    umap_all_table()
    input$umapPlotDimensions
  }
  ,
  handlerExpr = {
    table <- umap_all_table()
    dimensions <- input$umapPlotDimensions
    output$umapAllPlot <- renderPlotly(
      table %>%
        createPlotWithUmap(title = 'UMAP clustering of all cell types<br> (no sig threshold)', dimensions = dimensions) %>%
        layout(dragmode = 'select') # to make selection by defaul
    )
  })
# umap.selection <- reactive({
#   req(umap_all_table())
#   event_data('plotly_selected', source = 'umap_selection')
# })

##########################################################
umap_hypG_table <- reactiveVal()
observeEvent(rv$umap_hypG_file, {
  req(rv$umap_hypG_file)
  table = fread(rv$umap_hypG_file, header = TRUE, sep = "\t", showProgress = TRUE)
  umap_hypG_table(table)
})
observeEvent(
  eventExpr = {
    umap_hypG_table()
    input$umapPlotDimensions
  }
  ,handlerExpr = {
    dimensions <- input$umapPlotDimensions
    output$umapHypGPlot <- renderPlotly({
      umap_hypG_table() %>%
        createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> by hypergeometric test (p<0.05)', dimensions = dimensions)
    }
    )
  })

umap_KStest_table <- reactiveVal()
observeEvent(  rv$umap_KStest_file, {
  req(rv$umap_KStest_file)
  table = fread(rv$umap_KStest_file, header = TRUE, sep = "\t", showProgress = TRUE)
  umap_KStest_table(table)
})
observeEvent(
  eventExpr = {
    umap_KStest_table()
    input$umapPlotDimensions
  },
  handlerExpr = {
    dimensions <- input$umapPlotDimensions
    output$umapKSTestPlot <- renderPlotly(
      umap_KStest_table() %>%
        createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> by K-S test', dimensions = dimensions)
    )
  })

umap_sig001_table <- reactiveVal()
observeEvent(rv$umap_sig001_file, {
  req(rv$umap_sig001_file)
  table = fread(rv$umap_sig001_file, header = TRUE, sep = "\t", showProgress = TRUE)
  umap_sig001_table(table)
})
observeEvent(
  eventExpr = {
    umap_sig001_table()
    input$umapPlotDimensions
  },
  handlerExpr = {
    dimensions <- input$umapPlotDimensions
    output$umapSig001Plot <- renderPlotly(
      umap_sig001_table() %>%
        createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> (sig<0.01)', dimensions = dimensions)
    )
  })

umap_sig005_table <- reactiveVal()
observeEvent(rv$umap_sig005_file, {
  req(rv$umap_sig005_file)
  table = fread(rv$umap_sig005_file, header = TRUE, sep = "\t", showProgress = TRUE)
  umap_sig005_table(table)
})
observeEvent(
  eventExpr = {
    umap_sig005_table()
    input$umapPlotDimensions
  },
  handlerExpr = {
    dimensions <- input$umapPlotDimensions
    output$umapSig005Plot <- renderPlotly(
      umap_sig005_table() %>%
        createPlotWithUmap(title = 'UMAP clustering of significant cell types<br> (sig<0.05)', dimensions = dimensions)
    )
  })


