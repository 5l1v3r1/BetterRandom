#!/bin/sh
rm -r betterrandom/target/site/apidocs
rm -r docs/betterrandom-java7/io
echo '<!DOCTYPE html><html><head /><body style="font-family: sans-serif;">' > betterrandom/src/main/javadoc/overview.html
ruby ./render-readme-for-javadoc.rb >> betterrandom/src/main/javadoc/overview.html
echo '</body></html>' >> betterrandom/src/main/javadoc/overview.html
cd betterrandom
mvn javadoc:javadoc
rm src/main/javadoc/overview.html # Only needed temporarily
cd ..
cp -r betterrandom/target/site/apidocs/* docs/betterrandom-java7
cd docs
git checkout master
git pull
cd betterrandom-java7

# Disable frames, step 1
mv overview-summary.html index.html

# Create sitemap
find . -iname "*.html" | sed 's/^\./https:\/\/pr0methean.github.io/' > sitemap.txt

# AdSense, Analytics, Tag Manager
replace '<head>' "<head><script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':\
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],\
j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=\
'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);\
})(window,document,'script','dataLayer','GTM-K9NNCTT');</script>\
<script async src='//pagead2.googlesyndication.com\/pagead\/js\/adsbygoogle.js'></script>\
<script>(adsbygoogle = window.adsbygoogle || []).push({google_ad_client: 'ca-pub-9922551172827508', \
enable_page_level_ads: true});</script>" -- index.html

# Disable frames, step 2
find . -iname "*.html" -exec sed -i 's/<li><a href="[^\"]*" target="_top">Frames<\/a><\/li>//; s/<li><a href="[^\"]*" target="_top">No&nbsp;Frames<\/a><\/li>//; s/overview-summary.html/index.html/g' {} \;
git add .
git commit -m "🤖 Update Javadocs for GitHub Pages"
git branch
git pull --commit
git push
cd ../..
git submodule update --remote
