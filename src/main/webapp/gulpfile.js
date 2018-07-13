var gulp = require('gulp'),
    sass = require('gulp-sass'),
    filter = require('gulp-filter'),
    autoprefixer = require('gulp-autoprefixer'),
    uglify = require('gulp-uglify'),
    imagemin = require('gulp-imagemin'),
    pump = require('pump'),
    concat = require('gulp-concat');

var config = {
    stylesPath: 'styles/sass',
    jsPath: 'styles/js',
    localLibsPath: 'styles/libs',
    imagesPath: 'images',
    outputDir: 'assets'
};

// DEPENDENCIES

gulp.task('popper', function (cb) {
    pump([
        gulp.src('./node_modules/popper.js/dist/umd/*'),
        filter('**/popper.min.js*'),
        gulp.dest(config.outputDir + '/js')
    ], cb)
});

gulp.task('bootstrap-css', function (cb) {
    pump([
        gulp.src('./node_modules/bootstrap/dist/css/*'),
        filter('**/*.min.*'),
        concat('bootstrap.css'),
        gulp.dest(config.outputDir + '/css')
    ], cb);
});

gulp.task('bootstrap-js', gulp.parallel('popper', function (cb) {
    pump([
        gulp.src('./node_modules/bootstrap/dist/js/*'),
        filter('**/bootstrap.min.js*'),
        gulp.dest(config.outputDir + '/js')
    ], cb);
}));


gulp.task('font-awesome-js', function (cb) {
    pump([
        gulp.src(config.localLibsPath + '/*'),
        filter('**/fontawesome-all.js'),
        uglify(),
        concat('fontawesome.min.js'),
        gulp.dest(config.outputDir + '/js')
    ], cb);
});

gulp.task('jquery', function (cb) {
    pump([
        gulp.src('./node_modules/jquery/dist/*'),
        filter('**/jquery.min.*'),
        gulp.dest(config.outputDir + '/js')
    ], cb);
});

gulp.task('sweet-alerts', function () {
    return gulp.src('./node_modules/sweetalert/dist/sweetalert.min.js')
        .pipe(gulp.dest(config.outputDir + '/js'));
});

gulp.task('sorttable', function (cb) {
    pump([
        gulp.src(config.localLibsPath + '/*'),
        filter('**/sorttable.js'),
        uglify(),
        concat('sorttable.min.js'),
        gulp.dest(config.outputDir + '/js')
    ], cb);
});

gulp.task('loading-overlay', function (cb) {
    pump([
        gulp.src('./node_modules/gasparesganga-jquery-loading-overlay/dist/*'),
        filter('**/loadingoverlay.min.js*'),
        gulp.dest(config.outputDir + '/js')
    ], cb);
});

gulp.task('libs', gulp.parallel('bootstrap-css', 'bootstrap-js', 'jquery', 'font-awesome-js', 'sweet-alerts', 'sorttable', 'loading-overlay'));

// IMAGES
gulp.task('images', function () {
    return gulp.src(config.imagesPath + '/*')
        .pipe(imagemin())
        .pipe(gulp.dest(config.outputDir + '/images'))
});


// CSS
gulp.task('css', function (cb) {
    pump([
        gulp.src(config.stylesPath + '/**/*.scss'),
        sass({
            outputStyle: 'compressed',
            includePaths: [
                config.stylesPath,
                './node_modules/bootstrap/scss'
            ]
        }).on('error', sass.logError),
        autoprefixer(),
        gulp.dest(config.outputDir + '/css')
    ], cb);
});

// JS
gulp.task('js', gulp.series('libs', function (cb) {
    pump([
            gulp.src(config.jsPath + '/*'),
            filter('**/*.js'),
            uglify(),
            concat("main.min.js"),
            gulp.dest(config.outputDir + '/js')
        ],
        cb
    );
}));

// WATCHES
gulp.task('watch', function () {
    gulp.watch([config.stylesPath + '**/*.scss', config.stylesPath + '**/*.sass', config.stylesPath + '**/*.css'], ['css']);
    gulp.watch([config.jsPath + '**/*.js'], ['js']);
    gulp.watch([config.imagesPath + '/**/*'], ['images']);
});

// BUILD TASKS
gulp.task('build', gulp.parallel('images', 'css', 'js'));
gulp.task('default', gulp.parallel('build'));